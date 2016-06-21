(ns full.core.edn-test
  (:require [clojure.test :refer :all]
            [full.core.edn :refer :all]
            [full.core.sugar :refer :all]
            [clj-time.core :as t]))


(def ^:private edn-data
  [:keyword {1 2} #{"a" "b"} (t/date-time 2014 1 2 3 4 5 600)])

(def ^:private edn-string
  "[:keyword {1 2} #{\"a\" \"b\"} #inst \"2014-01-02T03:04:05.600-00:00\"]")


(deftest edn-test
  (is (= (write-edn edn-data) edn-string))
  (is (= (read-edn edn-string) edn-data)))
