(ns full.core.dev
  "Debug and development helpers."
  (:require [full.core.log :as log]
            [clojure.string :as s]
            [clojure.stacktrace :as st]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [ns-tracker.core :as tracker]))


(defn <println [& args]
  (apply prn args)
  (last args))

(defmacro catch-log
  [& body]
  `(try ~@body (catch Throwable e# (log/error e# "Exception") (throw e#))))

(def print-st st/print-stack-trace)

(defn call-stack []
  (try (throw (Exception. "")) (catch Exception e (print-st e))))

(def lc "Log context format helper"
  (partial format "%15s>"))

(def bell-char (char 7))

(defmacro do-bell
  "Evaluates c, rings a terminal bell when done and returns the result."
  [c]
  (let [res c] (println bell-char) res))

; borrowed from midje.config. Thanks, @marick!
(defn running-in-repl? []
  (try
    (throw (Exception.))
    (catch Exception ex
      (->> (filter #(.contains (str %) "clojure.main$repl$read_eval_print")
                   (.getStackTrace ex))
           (seq) boolean))))


;;; Simple color printing

(def ^:private ansi-colors
  {:green "[32m"
   :red "[31m"
   :yellow "[33m"
   :blue "[34m"})

(defn ^:private colored-str [code s]
  (str "\u001b" (get ansi-colors code) (apply str s) "\u001b[0m"))

(defn green-str [& s] (colored-str :green s))
(defn red-str [& s] (colored-str :red s))
(defn yellow-str [& s] (colored-str :yellow s))
(defn blue-str [& s] (colored-str :blue s))


;;; File I/O

(defn- expand-tilde [f]
  (when-let [first-char (some-> f first)]
    (if (= \~ first-char)
      (str (System/getenv "HOME") (.substring f 1))
      f)))

(defn slurp-edn [f & opts]
  (edn/read-string (apply slurp (expand-tilde f) opts)))

(defn slurp-lines [f & opts]
  (s/split (apply slurp (expand-tilde f) opts) #"\n"))

(defn spit-pprint [f content & opts]
  (with-open [^java.io.Writer writer (apply clojure.java.io/writer (expand-tilde f) opts)]
    (pprint content writer)))

(defn pwd
  "Returns current working directory"
  []
  (.getAbsolutePath (java.io.File "")))


;;; Dynamic code reloading

(defn- check-namespace-changes [track on-reload]
  (some->> (track)
           (not-empty)
           ((fn [x] (log/info "✂ ------------------------------------------------------------------------------") x))
           (map (fn [ns-sym]
                  (try
                    (log/info "Reloading namespace:" ns-sym)
                    (require ns-sym :reload)
                    ns-sym
                    (catch Throwable e
                      (log/error "Error reloading namespace" ns-sym e)))))
           (filter identity)
           (doall)
           (not-empty)
           (on-reload))
  (Thread/sleep 500))

(defn start-nstracker
  "Automatically tracks source code changes in src and checkouts folder
  and reloads the changed namespaces."
  [& {:keys [directories on-reload]
      :or {directories ["src" "checkouts"], on-reload (fn [_])}}]
  (let [track (tracker/ns-tracker directories)]
    (doto
      (Thread.
        #(while true
          (check-namespace-changes track on-reload)))
      (.setDaemon true)
      (.start))))
