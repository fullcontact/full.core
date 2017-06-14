(ns full.core.sugar
  (:require
    [clojure.walk :refer [postwalk]]
    [clojure.string :as string]
    #?@(:cljs [[goog.string :as gstring]
               [goog.string.format]]))
  #?(:clj (:import (java.text Normalizer Normalizer$Form)
                   (org.apache.commons.codec.binary Hex)
                   (java.net URLEncoder)
                   (java.util.concurrent LinkedBlockingQueue))))


;;; Macro helpers

#?(:clj
   (do
     (defn- cljs-env?
       "Take the &env from a macro, and tell whether we are expanding into cljs."
       [env]
       (boolean (:ns env)))

     (defmacro if-cljs
       "Return then if we are generating cljs code and else for Clojure code.
       https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
       [then else]
       (if (cljs-env? &env) then else))))


;;; Map helpers

(defn ?assoc
  "Same as clojure.core/assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> (partition 2 kvs)
       (remove (comp nil? second))
       (map vec)
       (into (or m {}))))

(defn assoc-first
  "Replaces value of key `k` in map `m` with the first value  sequence
   first item from map given key to resultant map."
  [m k]
  (if-let [v (get m k)]
    (assoc m k (first v))
    m))

(defn remove-empty-val
  "Filter empty? values from map."
  [m]
  (into {} (filter (fn [[k v]] (and (some? v)
                                    (or (and (not (coll? v)) (not (string? v)))
                                        (seq v)))) m)))

(defn remove-nil-val
  "Filter nil values from a map m."
  [m]
  (into {} (remove (comp nil? val) m)))

(defn dissoc-in
  [m [k & ks]]
  (if ks
    (if-let [submap (get m k)]
      (assoc m k (dissoc-in submap ks))
      m)
    (dissoc m k)))

(defn move-in
  "Moves a value in nested assoc structure."
  [m from to]
  (-> (assoc-in m to (get-in m from))
      (dissoc-in from)))

(defn ?move-in
  "Moves a value in nested assoc structure, if it is not nil."
  [m from to]
  (if (get-in m from)
    (move-in m from to)
    m))

(defn ?update
  "Performs a clojure.core/update if the original or resulting value is truthy,
  otherwise dissoc key."
  [m k f & args]
  (if-let [newv (when-let [v (get m k)] (apply f v args))]
    (assoc m k newv)
    (dissoc m k)))

(defn ?update-in
  "Performs a clojure.core/update-in if the original or resulting value is
  truthy, otherwise dissoc key."
  [m [k & ks] f & args]
  (if ks
    (assoc m k (apply ?update-in (get m k) ks f args))
    (if-let [newv (when-let [v (get m k)] (apply f v args))]
      (assoc m k newv)
      (dissoc m k))))

(defn move-map-in [m f from to]
  (-> (assoc-in m to (f (get-in m from)))
      (dissoc-in from)))

(defn copy-in [m from to]
  (assoc-in m to (get-in m from)))

(defn map-map
  ([key-fn m] (map-map key-fn (fn [v] v) m))
  ([key-fn value-fn m]
   (letfn [(mapper [[k v]] [(key-fn k)  (value-fn v)])]
     (into {} (map mapper m)))))

(defn deep-merge
  "Deep merge two maps"
  [& values]
  (if (every? map? (filter identity values))
    (apply merge-with deep-merge values)
    (last values)))

(defn index-by
  "Returns a map of the values in coll indexed by the result of applying kf.
   A value mapping function vf can be provided too."
  ([kf coll]
   (index-by kf identity coll))
  ([kf vf coll]
   (into {} (map (juxt kf vf)) coll)))

(defn remap
  "Remap keys of `m` based on `mapping`."
  [m mapping]
  (into {} (map (fn [[key new-key]] [new-key (get m key)]) mapping)))

(defn map-value
  ([value-fn m]
   (into {} (for [[k v] m] [k (value-fn v)]))))

(defn mapply [f & args]
  (apply f (apply concat (butlast args) (last args))))

(defn ?hash-map
  "Creates a hash-map from all key value pairs where value is not nil."
  [& keyvals]
  (apply ?assoc {} keyvals))


;;; List helpers

(defn insert-at
  "Returns the sequence s with the item i inserted at 0-based index idx."
  [s idx i]
  (apply conj (into (empty s) (take idx s)) (cons i (nthrest s idx))))

(defn remove-at
  "Returns the sequence s with the element at 0-based index idx removed."
  [s idx]
  (let [vec-s (vec s)]
    (into (vec (take idx vec-s)) (nthrest vec-s (inc idx)))))

(defn replace-at
  "Returns the sequence s with the item at 0-based index idx."
  [s idx i]
  (apply conj (into (empty s) (take idx s)) (cons i (nthrest s (inc idx)))))

(defn ?conj
  "Same as conj, but skip the conj if v is falsey"
  [coll & xs]
  (apply conj coll (filter identity xs)))


;;;; Seq helpers

#?(:clj
   (defn pipe
     "Returns a vector containing a sequence that will read from the
      queue, and a function that inserts items into the queue."
     []
     (let [q (LinkedBlockingQueue.)
           EOQ (Object.)
           NIL (Object.)
           s (fn queue-seq []
               (lazy-seq (let [x (.take q)]
                           (when-not (= EOQ x)
                             (cons (when-not (= NIL x) x)
                                   (queue-seq))))))]
       [(s) (fn queue-put
              ([] (.put q EOQ))
              ([x] (.put q (or x NIL))))])))

(def all? (partial every? identity))

(defn filter-indexed [pred coll]
  (filter pred (map-indexed vector coll)))

; DEPRECATED: first + filter is clearer.
(defn ^:deprecated some-when
  "Similiar to some but returns matching value instead of predicates result."
  [pred coll]
  (some #(when (pred %) %) coll))

(defn idx-of
  "Similar to .indexOf, but works with lazy collections as well."
  [collection item]
  (or (->> collection
           (map-indexed vector)
           (some (fn [[i v]]
                   (when (= v item)
                     i))))
      -1))

(defn update-last
  "Updates last item in sequence s by applying mapping method m to it."
  ([s m]
    (if (seq s)
      (assoc s (dec (count s)) (m (last s)))
      s))
   ([s m & args]
    (if (seq s)
      (assoc s (dec (count s)) (apply m (last s) args))
      s)))

(defn update-first
  "Updates first item in sequence s by applying mapping method m to it."
  ([s m]
    (if (seq s)
      (assoc s 0 (m (first s)))
      s))
  ([s m & args]
    (if (seq s)
      (assoc s 0 (apply m (first s) args))
      s)))

(defn juxt-partition
  "Takes a predicate function, a collection and one ore more
   (fn predicate coll) functions that will be applied to the given collection.
   Example: (juxt-partition odd? [1 2 3 4] filter remove) => [(1 3) (2 4)]."
  [pred coll & fns]
  ((apply juxt (map #(partial % pred) fns)) coll))


;;; String helpers

(defn as-long [s]
  (when s
    #?(:clj (try
              (Long/parseLong (str s))
              (catch NumberFormatException _))
       :cljs (let [n (js/parseInt (str s))]
               (when-not (js/isNaN n)
                 n)))))

(defn number-or-string [s]
  (let [s (str s)]
    #?(:clj (try
              (Long/parseLong s)
              (catch Exception _ s))
       :cljs (let [n (js/parseInt (str s))]
               (if (js/isNaN n)
                 s
                 n)))))

(defn remove-prefix [s prefix]
  (if (and s (string/starts-with? s prefix))
    (subs s (count prefix))
    s))

(defn replace-prefix [s prefix new-prefix]
  (if (and s (string/starts-with? s prefix))
    (str new-prefix (subs s (count prefix)))
    s))

(defn remove-suffix [s suffix]
  (if (and s (string/ends-with? s suffix))
    (subs s 0 (- (count s) (count suffix)))
    s))

(defn dq
  "Converts single quotes to double quotes."
  [s]
  (string/replace s #"'" "\""))

(defn query-string [m]
  (string/join "&" (for [[k v] m]
                (str (name k) "=" #?(:clj (URLEncoder/encode (str v) "UTF-8")
                                     :cljs (js/encodeURIComponent (str v)))))))

(defn strip
  "Takes a string s and a string cs. Removes all cs characters from s."
  [s cs]
  (let [cs-set (set cs)]
    (apply str (remove cs-set s))))

(defn str-greater?
  "Returns true if this is greater than that. Case insensitive."
  [this that]
  (pos? (compare (string/lower-case this) (string/lower-case that))))

(defn str-smaller?
  "Returns true if this is smaller than that. Case insensitive."
  [this that]
  (neg? (compare (string/lower-case this) (string/lower-case that))))

(defn uuids
  "Generates UUID without dashes."
  []
  #?(:clj (string/replace (str (java.util.UUID/randomUUID)) "-" "")
     :cljs (letfn [(top-32-bits [] (.toString (int (/ (.getTime (js/Date.)) 1000)) 16))
                   (f [] (.toString (rand-int 16) 16))
                   (g [] (.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))]
             (string/join (concat (top-32-bits)
                                  (repeatedly 4 f) "4"
                                  (repeatedly 3 f)
                                  (g) (repeatedly 3 f)
                                  (repeatedly 12 f))))))

#?(:clj
   (do
     (defn ascii
       "Ensures all characters in the given string are converted to ASCII.
        For example: ā->a."
       [s]
       (.replaceAll (Normalizer/normalize s Normalizer$Form/NFD)
                    "\\p{InCombiningDiacriticalMarks}+"
                    ""))

     (defn byte-buffer->byte-vector [bb]
       (loop [byte-vector []]
         (if (= (.position bb) (.limit bb))
           byte-vector
           (recur (conj byte-vector (.get bb))))))

     (defn byte-buffer->hex-string [byte-buffer]
       (->> (byte-buffer->byte-vector byte-buffer)
            (into-array Byte/TYPE)
            (Hex/encodeHex)
            (string/join)))))

(defn re-quote
  "Returns a literal regex pattern for given string, similiar to Java's Pattern.quote."
  [s]
  ; from http://stackoverflow.com/a/11981277"
  (let [special (set ".?*+^$[]\\(){}|")
        escfn #(if (special %) (str \\ %) %)]
    (apply str (map escfn s))))

;;; Metadata helpers

(defn def-name
  "Returns human readable name of defined symbol (such as def or defn)."
  [sym]
  (-> `(name ~sym)
      (second)
      (str)
      (string/split #"\$")
      (last)
      (string/split #"@")
      (first)
      (string/replace #"__" "-")
      (string/replace #"GT_" ">")))

(defn mname
  "Meta name for the object."
  [obj]
  (-> obj meta :name))

#?(:clj
   (do
     (defmacro with-mname [meta-name body]
       `(with-meta ~body {:name ~meta-name}))

     (defmacro fn-name [meta-name args & body]
       `(with-mname ~meta-name (fn ~args ~@body)))

     (defmethod print-method clojure.lang.AFunction [v ^java.io.Writer w]
       ; if function has :name in metadata, this will make it appear in (print)
       (.write w (or (mname v) (str v))))))


;;;; Conditional threading

#?(:clj
   (do
     (defn- cndexpand [cnd value]
       (postwalk (fn [c] (if (= (symbol "%") c) value c)) cnd))

     (defmacro when->>
       "Using when + ->> inside ->> threads
        Takes a single condition and one or more forms that will be executed
        like a regular ->>, if condition is true.
        Will pass the initial value if condition is false.
        Contition can take the initial value as argument, it needs to be
        referenced as '%' (eg, (some-condition %)
        (->> (range 10) (map inc) (when->> true (filter even?)))
        => (2 4 6 8 10)"
       [cnd & threads]
       `(if ~(cndexpand cnd (last threads))
          (->> ~(last threads)
               ~@(butlast threads))
          ~(last threads)))

     (defmacro when->
       "Using when + -> inside -> threads
        Takes a single condition and multiple forms that will be executed like a
        normal -> if the condition is true.
        Contition can take the initial value as argument, it needs to be
        referenced as '%' (eg, (some-condition %)
        (-> \"foobar\" (upper-case) (when-> true (str \"baz\")))
        => FOOBARbaz"
       [thread cnd & threads]
       `(if ~(cndexpand cnd thread)
          (-> ~thread
              ~@threads)
          ~thread))

     (defmacro when->>->
       "Using when + -> inside ->> threads.
        Takes a single condition cnd and multiple forms that will be exectued as
        a regular ->>, if the condition is true (otherwise the initial value
        will be passed to next form).
        Contition can take the initial value as argument, it needs to be
        referenced as '%' (eg, (some-condition %)
       (->> (range 3) (map inc) (when->>-> (seq %) (into [\"header\"])))
       => (\"header\" 1 2 3)"
       [cnd & threads]
       `(if ~(cndexpand cnd (last threads))
          (-> ~(last threads)
              ~@(butlast threads))
          ~(last threads)))

     (defmacro if->>
       "Using if + ->> inside ->> threads
       Takes a single condition and one or more forms that
       will be executed if the condition is true.
       An else block can be passed in by separating forms with :else keyword.
        Contition can take the initial value as argument, it needs to be
        referenced as '%' (eg, (some-condition %)
       (->> (range 10) (if->> false
                              (filter odd?) (map inc)
                              :else (filter even?) (map dec)))
       => (-1 1 3 5 7)"
       [cnd & threads]
       `(if ~(cndexpand cnd (last threads))
          (->> ~(last threads)
               ~@(take-while #(not= :else %) (butlast threads)))
          (->> ~(last threads)
               ~@(rest (drop-while #(not= :else %) (butlast threads))))))

     (defmacro nest->
       "Allows to sneak in ->s inside ->>s.
       (->> (range 3) (map inc) (nest-> (nth 1) inc) (str \"x\"))
       => x3"
       [& threads]
       `(-> ~(last threads)
            ~@(butlast threads)))))


;;; Helpers for measuring execution time


(defn time-bookmark
  "Returns time bookmark (technically system time in nanoseconds).
  For use in concert with ellapsed-time to messure execution time
  of some code block."
  []
  #?(:clj (. System (nanoTime))
     :cljs (. (js/Date.) getTime)))

(defn ellapsed-time
  "Returns ellapsed time in milliseconds since the time bookmark."
  [time-bookmark]
  #?(:clj (/ (double (- (. System (nanoTime)) time-bookmark)) 1000000.0)
     :cljs (- (. (js/Date.) getTime) time-bookmark)))


;;;; Numbers


(defn format-opt-prec
  [n precision]
  (let [f (str "%." precision "f")]
    (loop [v #?(:clj (format f n)
                :cljs (gstring/format f n))]
      (if (pos? precision)
        (let [length (count v)
              lc (nth v (dec length))]
          (if (= lc \0)
            (recur (subs v 0 (dec length)))
            (if (= lc \.)
              (subs v 0 (dec length))
              v)))
        v))))

(defn num->compact
  [n & {:keys [prefix suffix]}]
  (when n
    (let [n (double n)
          abs (max n (- n))]
      (str
        (if (neg? n) "-" "")
        prefix
        (cond
          (> 10 abs) (format-opt-prec abs 2)
          (> 100 abs) (format-opt-prec abs 1)
          (> 1000 abs) (format-opt-prec abs 0)
          (> 10000 abs) (str (format-opt-prec (/ abs 1000) 2) "K")
          (> 100000 abs) (str (format-opt-prec (/ abs 1000) 1) "K")
          (> 1000000 abs) (str (format-opt-prec (/ abs 1000) 0) "K")
          (> 10000000 abs) (str (format-opt-prec (/ abs 1000000) 2) "M")
          (> 100000000 abs) (str (format-opt-prec (/ abs 1000000) 1) "M")
          (> 1000000000 abs) (str (format-opt-prec (/ abs 1000000) 0) "M")
          (> 10000000000 abs) (str (format-opt-prec (/ abs 1000000000) 2) "B")
          (> 100000000000 abs) (str (format-opt-prec (/ abs 1000000000) 1) "B")
          (> 1000000000000 abs) (str (format-opt-prec (/ abs 1000000000) 0) "B")
          :else (str (format-opt-prec (/ abs 1000000000000) 2) "T"))
        suffix))))
