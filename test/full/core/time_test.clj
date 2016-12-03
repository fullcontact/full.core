(ns full.core.time-test
  (:require [clojure.test :refer :all]
            [full.core.time :refer :all]
            [clj-time.core :as t]))


(deftest test-iso-dt-parsing
  (is (= (dt<-iso-ts "2014-01-02T03:04:05Z") (t/date-time 2014 1 2 3 4 5)))
  (is (nil? (dt<-iso-ts "2014-01-02T03:04:04Zdsfffsdf")))
  (is (= (t/date-time 2014 1 2 3 4 5 678) (dt<-iso-ts "2014-01-02T03:04:05.678Z")))
  (is (= (dt->iso-ts (t/date-time 2014 1 2 3 4 5 678))  "2014-01-02T03:04:05.678Z"))
  (is (= (dt->iso-ts (t/date-time 2014 1 2 3 4 5))  "2014-01-02T03:04:05.000Z"))
  (is (= (d<-iso-d "2014-01-02") (t/local-date 2014 1 2)))
  (is (= (d<-iso-d "0000-01-02") (t/local-date 0 1 2)))
  (is (= (d<-iso-d "--01-02") (t/local-date 0 1 2)))
  (is (= (d<-iso-d "12-26") (t/local-date 0 12 26)))
  (is (= (d<-iso-d "2014-02") (t/local-date 2014 2 1)))
  (is (= (d<-iso-d "2014") (t/local-date 2014 1 1)))
  (is (= (d->iso-d (t/local-date 2014 1 2)) "2014-01-02"))
  (is (= (d->iso-d (t/local-date 0 1 2)) "0000-01-02")))

(deftest rfc822-date-parsing
  (is (= (str (dt<-rfc822-ts "Fri, 12 Jun 2015 17:50:00 UTC"))
         (str (t/date-time 2015 6 12 17 50 0 0)))))

(deftest relative-date-formatting
  (is (= (-> (t/now) (t/minus (t/seconds 10)) (dt->rel)) "few seconds ago"))
  (is (= (-> (t/now) (t/minus (t/minutes 10)) (dt->rel)) "10mi ago"))
  (is (= (-> (t/now) (t/plus (t/seconds 130)) (dt->rel)) "in 2mi"))

  (is (= (-> (t/now) (t/minus (t/hours 1)) (t/minus (t/minutes 10)) (dt->rel))
         "1h 10mi ago"))

  (is (= (-> (t/now) (t/minus (t/days 1)) (t/minus (t/hours 1))
             (t/minus (t/minutes 10)) (dt->rel))
         "1d 1h ago"))

  (is (= (-> (t/now) (t/minus (t/days 10)) (t/minus (t/hours 1))
             (t/minus (t/minutes 10)) (dt->rel))
         "1w 3d ago"))

  (is (= (-> (t/now) (t/minus (t/days 14)) (t/minus (t/hours 1))
            (t/minus (t/minutes 10)) (dt->rel))
         "2w ago"))

  (is (= (-> (t/now) (t/minus (t/days 40)) (t/minus (t/hours 1))
            (t/minus (t/minutes 10)) (dt->rel))
         "1mo 1w ago"))
  (is (= (-> (t/now) (t/minus (t/days 70)) (t/minus (t/hours 1))
            (t/minus (t/minutes 10)) (dt->rel))
         "2mo 1w ago"))

  (is (= (-> (t/now) (t/minus (t/days 400)) (t/minus (t/hours 1))
             (t/minus (t/minutes 10)) (dt->rel))
         "1y 1mo ago"))

  (is (= (-> (t/now) (t/minus (t/days 800)) (t/minus (t/hours 1))
            (t/minus (t/minutes 10)) (dt->rel))
         "2y 2mo ago")))
