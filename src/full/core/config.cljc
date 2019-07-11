(ns full.core.config
  (:require
    [full.core.sugar :refer [map-map]]
    #?@(:clj [[clojure.walk :refer [prewalk]]
              [clojure.tools.cli :refer [parse-opts]]
              [clojure.java.io :refer [as-file]]
              [clj-yaml.core :as yaml]])))

(defonce _config (atom {}))

(defn- normalize-config [config]
  (map-map keyword config))

#?(:clj
   (do
     (def config-cli-options
       [["-c" "--config name" "Config filename"]])

     (def ^:private re-env #"^\$\{([A-Z_0-9]+)\}$")

     (defn- with-env-varialbes [config]
       (prewalk #(if-let [[_ v] (when (string? %)
                                 (re-matches re-env %))]
                  (System/getenv v) %)
                config))

     (defn config-file
       "Loads config from a file. Path is taken either from command line -c flag,
        FULL_CONFIG env variable, or the default `dev.yaml` is used."
       []
       (let [f (-> (parse-opts *command-line-args* config-cli-options)
                   (:options) :config
                   ; or env variable
                   (or (System/getenv "FULL_CONFIG"))
                   ; of use dev.yaml as default
                   (or "dev.yaml")
                   (as-file))]
         (when-not (.exists f)
           (println "full.core.config - EXITING! - Configuration file"
                    (.getAbsolutePath f) "not found.")
           (System/exit 1))
         (println "full.core.config - Using config file" (.getAbsolutePath f))
         f))
     ))

(defn configure
  ([config] (reset! _config (normalize-config config)))
  #?(:clj ([] (swap! _config (fn [config]
                               ; only load config once
                               (if (empty? config)
                                 (-> (config-file)
                                     (slurp)
                                     (yaml/parse-string)
                                     (with-env-varialbes)
                                     (normalize-config))
                                 ; else - already configured, return exisiting config
                                 config))))))

::undefined

(defn opt
  "Yields a lazy configuration value, readable by dereferencing with @. Will
  throw an exception when no value is present in configuration and no default
  value is specified.

  Parameters:
    sel - a keyword or vector of keywords representing path in config file
    :default - default value. Use `nil` for optional configuration.
    :mapper -  function to apply to configuration value before returning
  "
  [sel & {:keys [default mapper]
          :or {default ::undefined}}]
  {:pre [(or (keyword? sel)
             (and (vector? sel)
                  (every? keyword sel)))
         (or (nil? mapper)
             (fn? mapper))]}
  (delay
    (let [conf-value (if (vector? sel) (get-in @_config sel) (get @_config sel))
          value (if (some? conf-value) conf-value default)]
      (when (= ::undefined value)
        (let [m (str "Option " sel " is not configured")]
          (throw #?(:clj (RuntimeException. m)
                    :cljs (js/Error. m)))))
      (if mapper
        (mapper value)
        value))))
