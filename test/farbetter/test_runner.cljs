(ns farbetter.test-runner
  (:require
   [cljs.nodejs :as nodejs]
   [cljs.test :as test :refer-macros [run-tests]]
   [farbetter.mealy-test]))

(nodejs/enable-util-print!)

(defn -main [& args]
  (run-tests 'farbetter.mealy-test))

(set! *main-cli-fn* -main)
