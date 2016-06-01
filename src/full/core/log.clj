(ns full.core.log
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :refer [as-file as-url]]
            [full.core.config :refer [opt]])
  (:import (org.slf4j MDC)))


(def ^:private log-config (opt :log-config :default "log.xml"))
(def ^:dynamic context "")


(defmacro with-prefix
  [context & body]
  `(binding [context ~context]
     ~@body))

(defmacro with-mdc
  "Adds an MDC map to any logging wrapped in the macro.  Macro can be nested.
   Note that this won't work well across go blocks since execution switches
   between threads and doesn't pass through thead locals.
  (with-mdc {:key \"value\"} (log/info \"yay\"))"
  [context & body]
  `(let [wrapped-context# ~context
         ctx# (MDC/getCopyOfContextMap)]
     (try
       (if (map? wrapped-context#)
         (doall (map (fn [[k# v#]] (MDC/put (name k#) (str v#)))
                wrapped-context#)))
       ~@body
       (finally
         (if ctx#
           (MDC/setContextMap ctx#)
           (MDC/clear))))))

(defmacro trace
  "Logs args with trace loglevel. Returns nil."
  [& args]
  `(log/trace context ~@args))

(defmacro debug
  "Logs args with debug loglevel. Returns nil."
  [& args]
  `(log/debug context ~@args))

(defmacro info
  "Logs args with info loglevel. Returns nil."
  [& args]
  `(log/info context ~@args))

(defmacro warn [& args]
  `(log/warn context ~@args))

(defmacro error [x & more]
  `(let [x# ~x]
     (if (instance? Throwable x#)
       (log/error x# context ~@more)
       (log/error context x# ~@more))))

(defmacro level-enabled? [level]
  `(log/enabled? ~level))

(defn do-info
  "Evaluates all arguments and logs them with info loglevel. Returns the value
   of the last argument."
  [& args]
    (info (clojure.string/join ", " args))
    (last args))

(defn do-debug
  "Evaluates all arguments and logs them with debug loglevel. Returns the value
   of the last argument."
  [& args]
    (debug (clojure.string/join ", " args))
    (last args))


;;; Configuration

(defn check-config-file [config-file]
  (let [f (as-file config-file)]
    (when (not (.exists f))
      (println "full.core.log - EXITING!- Log configuration file"
               (.getAbsolutePath f)
               "not found.")
      (System/exit 1))
    (println "full.core.log - Using log config file" (.getAbsolutePath f))
    f))

(defn configure
  ; TODO: If we make this as a macro, would it be possible to load this
  ;       module without the logback dependency?
  ([] (configure @log-config))
  ([config-file]
   (let [context (org.slf4j.LoggerFactory/getILoggerFactory)]
     (try
       (let [configurator (ch.qos.logback.classic.joran.JoranConfigurator.)]
         (.setContext configurator context)
         (.reset context)
         (.doConfigure configurator (as-url (check-config-file config-file))))
       (catch Exception _))  ; StatusPrinter will handle this
     (ch.qos.logback.core.util.StatusPrinter/printInCaseOfErrorsOrWarnings
      context))))
