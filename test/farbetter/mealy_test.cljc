(ns farbetter.mealy-test
  (:require
   #?(:cljs [cljs.test :as t])
   [#?(:clj clojure.core.async :cljs cljs.core.async)
    :refer [alts! chan close! take! timeout <! >! #?@(:clj [go <!! >!!])]]
   #?(:clj [clojure.test :refer [deftest is]])
   [farbetter.mealy :refer [make-state-machine start]]
   #?(:clj [farbetter.mealy.macros :refer [<!? <!!?]])
   [farbetter.utils :refer [test-async test-within-ms]]
   [taoensso.timbre #?(:clj :refer :cljs :refer-macros) [debugf errorf infof]])
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :refer [go]]
      [cljs.test :refer [async deftest is]]
      [farbetter.mealy.macros :refer [<!? <!!?]])))

(defn make-state-map [output-chan]
  {:start (fn [current-state input]
            (go
              (>! output-chan (str "Got " input)))
            (case input
              "2" :state2
              "3" :state3
              :done nil
              :unexpected))
   :state2 (fn [current-state input]
             (go
               (>! output-chan (str "Got " input)))
             (case input
               "3" :state3
               :done nil
               :unexpected))
   :state3 (fn [current-state input]
             (go
               (>! output-chan (str "Got " input)))
             (case input
               :done nil
               :unexpected))
   :unexpected (fn [current-state input]
                 (>! output-chan "Got unexpected input"))})

(deftest test-basics
  (let [input-chan (chan)
        output-chan (chan)
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan)]
    (start state-machine)
    (test-async
     (go
       (>! input-chan "2")
       (is (= "Got 2" (<! output-chan)))
       (>! input-chan "3")
       (is (=  "Got 3" (<! output-chan)))
       (>! input-chan :done)
       (is (= "Got :done" (<! output-chan)))))))

(deftest test-timeout
  (let [input-chan (chan)
        output-chan (chan)
        timeout-ms 10
        tof (fn [current-state]
              (go
                (>! output-chan "TIMEOUT!!!!!!!!"))
              nil)
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan
                       :timeout-ms timeout-ms
                       :timeout-fn tof)]
    (start state-machine)
    (test-async
     (go
       (>! input-chan "2")
       (is (= "Got 2" (<! output-chan)))
       (<! (timeout (* timeout-ms 3))) ;; Wait for the timeout
       (is (=  "TIMEOUT!!!!!!!!"(<! output-chan)))))))

(deftest test-shutdown-fn
  (let [input-chan (chan)
        output-chan (chan)
        shutdown-chan (chan)
        shutdown-fn (fn []
                      (go
                        (>! shutdown-chan "Shutting down")))
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan
                       :shutdown-fn shutdown-fn)]
    (start state-machine)
    (test-async
     (go
       (>! input-chan :done)
       (is (= "Shutting down" (<! shutdown-chan)))))))

(deftest test-timeout-w-no-timeout-fn
  (let [input-chan (chan)
        output-chan (chan)
        timeout-ms 10
        shutdown-fn (fn []
                      (go
                        (>! output-chan "Shutting down")))
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan
                       :timeout-ms timeout-ms
                       :shutdown-fn shutdown-fn)]
    (start state-machine)
    (go
      (>! input-chan "2")
      (is (= "Got 2" (<! output-chan)))
      (<! (timeout (* timeout-ms 3))) ;; Wait for the timeout
      (is (= "Shutting down" (<! output-chan))))))

(deftest test-non-existent-state
  (let [input-chan (chan)
        state-map {:start (constantly :non-existent-state)}
        error-chan (chan)
        state-machine (make-state-machine
                       state-map
                       input-chan
                       :error-fn (fn [err]
                                   (go
                                     (>! error-chan err))))]
    (start state-machine)
    (test-async
     (go
       (>! input-chan "some input")
       (is (= (ex-data (<! error-chan))
              {:type :nonexistent-next-state,
               :next-state :non-existent-state}))))))

(deftest test-<!!?
  (let [test-chan (chan 1)
        ex (ex-info "test" {:type :test})]
    (test-async
     (go
       (>! test-chan ex)
       (is (thrown-with-msg? #?(:clj
                                clojure.lang.ExceptionInfo
                                :cljs cljs.core/ExceptionInfo)
                             #"test"
                             (<!? test-chan)))))))
