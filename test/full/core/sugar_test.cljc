(ns full.core.sugar-test
  #?(:clj (:require [clojure.test :refer :all]
                    [full.core.sugar :refer :all])
     :cljs (:require [cljs.test :refer-macros [deftest is]]
                     [full.core.sugar
                      :refer [?assoc insert-at remove-at ?conj ?hash-map update-first update-last ?update-in ?update
                              juxt-partition as-long number-or-string remove-prefix replace-prefix remove-suffix dq
                              query-string num->compact re-quote index-by idx-of]
                      :refer-macros [when->> when->]])))

(deftest test-?assoc
  (is (= (?assoc {} :foo "bar") {:foo "bar"}))
  (is (= (?assoc {} :foo "bar") {:foo "bar"}))
  (is (= (?assoc {:foo "bar"} :foo "baz") {:foo "baz"}))
  (is (= (?assoc {:foo "bar"} :foo nil) {:foo "bar"}))
  (is (= (?assoc {} :empty nil) {})))

(deftest test-index-by
  (is (= (index-by :id nil) {}))
  (is (= (index-by :id []) {}))
  (is (= (index-by :id [{:id 1 :name "A"}
                        {:id 2 :name "B"}
                        {:id 3 :name "C"}
                        {:id 2 :name "D"}])
         {1 {:id 1 :name "A"}
          2 {:id 2 :name "D"}
          3 {:id 3 :name "C"}}))
  (is (= (index-by :id :name [{:id 1 :name "A"}
                              {:id 2 :name "B"}
                              {:id 3 :name "C"}
                              {:id 2 :name "D"}])
         {1 "A" 2 "D" 3 "C"})))

(deftest test-remove-prefix
  (is (= (remove-prefix "aaabbb" "aaa") "bbb"))
  (is (= (remove-prefix "aaabbb" "ccc") "aaabbb"))
  (is (= (remove-prefix nil "ccc") nil)))

(deftest test-replace-prefix
  (is (= (replace-prefix "aaabbb" "aaa" "1") "1bbb"))
  (is (= (replace-prefix "aaabbb" "ccc" "2") "aaabbb"))
  (is (= (replace-prefix nil "ccc" "3") nil)))

(deftest test-remove-suffix
   (is (= (remove-suffix "aaabbb" "bbb") "aaa"))
   (is (= (remove-suffix "aaabbb" "ccc") "aaabbb"))
   (is (= (remove-suffix nil "ccc") nil)))

(deftest test-dq
  (is (= (dq "'x'") "\"x\"")))

(deftest test-query-string
  (is (= (query-string {:a "x" :b 1}) "a=x&b=1")))

#?(:clj
   (deftest test-ascii
     (is (= (ascii "ĀČĒāčēAce") "ACEaceAce"))))

(deftest test-insert-at
  (is (= (insert-at [] 0 "x") ["x"]))
  (is (= (insert-at '() 0 "x") '("x")))
  (is (= (insert-at [] 5 "x") ["x"]))
  (is (= (insert-at [1 2 3 4] 0 0) [0 1 2 3 4]))
  (is (= (insert-at [1 2 3 4] 1 0) [1 0 2 3 4]))
  (is (= (insert-at [1 2 3 4] 3 0) [1 2 3 0 4]))
  (is (= (insert-at [1 2 3 4] 5 0) [1 2 3 4 0])))

(deftest test-remove-at
  (is (= (remove-at [] 0) []))
  (is (= (remove-at '() 5) '()))
  (is (= (remove-at '(1 1 2 3) 1) '(1 2 3)))
  (is (= (remove-at ["a"] 0) []))
  (is (= (remove-at [1 2 3 4] 0) [2 3 4]))
  (is (= (remove-at [1 2 3 4] 1) [1 3 4]))
  (is (= (remove-at [1 2 3 4] 3) [1 2 3]))
  (is (= (remove-at [1 2 3 4] 5) [1 2 3 4])))

(deftest test-?conj
  (is (= (?conj [] 1) [1]))
  (is (= (?conj [] nil) []))
  (is (= (?conj [1] 2 3) [1 2 3]))
  (is (= (?conj [1] nil 3) [1 3])))

(deftest test-conditional-threading
  (is (= (->> (range 10)
              (map inc)
              (when->> true (filter even?)))
         '(2 4 6 8 10)))
  (is (= (->> (range 10)
              (map inc)
              (when->> false (filter even?)))
         '(1 2 3 4 5 6 7 8 9 10)))
  (is (= (-> "foobar"
             (clojure.string/upper-case)
             (when-> true (str "baz")))
         "FOOBARbaz"))
  (is (= (-> "foobar"
             (clojure.string/upper-case)
             (when-> false (str "baz")))
         "FOOBAR")))

(deftest test-?hash-map
  (is (= (?hash-map :foo nil :bar nil :baz "xx") {:baz "xx"}))
  (is (= (?hash-map :foo nil ) {})))

(deftest test-update-last
  (is (= (update-last [] inc) []))
  (is (= (update-last [1] inc) [2]))
  (is (= (update-last [1 2 3] inc) [1 2 4]))
  (is (= (update-last [1 2 3] + 10) [1 2 13]))
  (is (= (update-last [1 2 3] + 10 20) [1 2 33])))

(deftest test-update-first
  (is (= (update-first [] inc) []))
  (is (= (update-first [1] inc) [2]))
  (is (= (update-first [1 2 3] inc) [2 2 3]))
  (is (= (update-first [1 2 3] + 10) [11 2 3]))
  (is (= (update-first [1 2 3] + 10 20) [31 2 3])))

(deftest test-num->compact
  (is (= (num->compact 0.1) "0.1"))
  (is (= (num->compact 0.11) "0.11"))
  (is (= (num->compact 0.19) "0.19"))
  (is (= (num->compact 0.191) "0.19"))
  (is (= (num->compact 0.199) "0.2"))
  (is (= (num->compact 1) "1"))
  (is (= (num->compact 1.12) "1.12"))
  (is (= (num->compact 10.12) "10.1"))
  (is (= (num->compact 100.12) "100"))
  (is (= (num->compact 1000) "1K"))
  (is (= (num->compact 1290) "1.29K"))
  (is (= (num->compact 1029) "1.03K"))
  (is (= (num->compact 10290) "10.3K"))
  (is (= (num->compact 102900) "103K"))
  (is (= (num->compact 950050) "950K"))
  (is (= (num->compact 1000000) "1M"))
  (is (= (num->compact 1200000) "1.2M"))
  (is (= (num->compact 1251000) "1.25M"))
  (is (= (num->compact 11251000) "11.3M"))
  (is (= (num->compact 911251000) "911M"))
  (is (= (num->compact 1911251000) "1.91B"))
  (is (= (num->compact 11911251000) "11.9B"))
  (is (= (num->compact 119112510000) "119B"))
  (is (= (num->compact 1191125100000) "1.19T")))

(deftest test-?update-in
  (is (= (?update-in {} [:foo] inc) {}))
  (is (= (?update-in {:foo 0} [:foo] inc) {:foo 1}))
  (is (= (?update-in {:foo 0} [:foo] (constantly nil)) {}))
  (is (= (?update-in {:foo {:bar  0}} [:foo :bar] inc) {:foo {:bar 1}}))
  (is (= (?update-in {:foo {:bar 0}} [:foo :bar] (constantly nil)) {:foo {}})))

(deftest test-?update
  (is (= (?update {} :foo inc) {}))
  (is (= (?update {:foo 0} :foo inc) {:foo 1}))
  (is (= (?update {:foo 0} :foo (constantly nil)) {})))

(deftest test-juxt-partition
  (is (= (juxt-partition odd? [1 2 3 4] filter remove) ['(1 3) '(2 4)]))
  (is (= (juxt-partition odd? [1 2 3 4] remove filter) ['(2 4) '(1 3)])))

(deftest test-as-long
  (is (= (as-long "123") 123))
  (is (= (as-long 123) 123))
  (is (= (as-long "abc") nil)))

(deftest test-number-or-string
  (is (= (number-or-string "123") 123))
  (is (= (number-or-string 123) 123))
  (is (= (number-or-string "abc") "abc")))

(deftest test-re-quote
  (is (= (re-quote "$1") "\\$1")))

(deftest test-idx-of
  (is (= 0 (idx-of (range 1 10) 1)))
  (is (= -1 (idx-of (range 1 10) 11)))
  (is (= 8 (idx-of (range 1 10) 9))))
