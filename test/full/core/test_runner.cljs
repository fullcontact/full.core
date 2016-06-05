(ns full.core.test-runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [full.core.sugar-test]))

(doo-tests 'full.core.sugar-test)