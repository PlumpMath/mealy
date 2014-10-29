(ns farbetter.mealy-test
  #+clj  (:use clojure.test)
  #+cljs (:use-macros
          [cemerick.cljs.test :only [is deftest]])
  #+cljs (:require-macros [cljs.core.async.macros :as am :refer [go]])
  (:require [#+clj clojure.core.async #+cljs cljs.core.async
             :refer [>! <! chan timeout #+clj go]]
            [farbetter.mealy :refer [make-state-machine start]]
            #+cljs [cemerick.cljs.test :as t]))

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
                 (go
                   (>! output-chan "Got unexpected input")))})

(deftest test-basics
  (let [input-chan (chan)
        output-chan (chan)
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan)]
    (start state-machine)
    (go
      (>! input-chan "2")
      (is (= "Got 2" (<! output-chan)))
      (>! input-chan "3")
      (is (=  "Got 3"(<! output-chan)))
      (>! input-chan :done)
      (is (= "Got :done" (<! output-chan))))))

;; (deftest test-timeout
;;   (let [input-chan (chan)
;;         output-chan (chan)
;;         timeout-ms 10
;;         tof (fn [current-state]
;;               (go
;;                 (>! output-chan "TIMEOUT!!!!!!!!"))
;;               nil)
;;         state-machine (make-state-machine
;;                        (make-state-map output-chan) input-chan
;;                        :timeout-ms timeout-ms
;;                        :timeout-fn tof)]
;;     (start state-machine)
;;     (go
;;       (>! input-chan "2")
;;       (is (= "Got 2" (<! output-chan)))
;;       (<! (timeout (* timeout-ms 3))) ;; Wait for the timeout
;;       (is (=  "TIMEOUT!!!!!!!!"(<! output-chan))))))

;; (deftest test-shutdown-fn
;;   (let [input-chan (chan)
;;         output-chan (chan)
;;         shutdown-fn (fn []
;;                       (go
;;                         (>! output-chan "Shutting down")))
;;         state-machine (make-state-machine
;;                        (make-state-map output-chan) input-chan
;;                        :shutdown-fn shutdown-fn)]
;;     (start state-machine)
;;     (go
;;       (>! input-chan "2")
;;       (is (= "Got 2" (<! output-chan)))
;;       (>! input-chan :done)
;;       (is (= "Got :done" (<! output-chan)))
;;       (is (= "Shutting down" (<! output-chan))))))

;; (deftest test-timeout-w-no-timeout-fn
;;   (let [input-chan (chan)
;;         output-chan (chan)
;;         timeout-ms 10
;;         shutdown-fn (fn []
;;                       (go
;;                         (>! output-chan "Shutting down")))
;;         state-machine (make-state-machine
;;                        (make-state-map output-chan) input-chan
;;                        :timeout-ms timeout-ms
;;                        :shutdown-fn shutdown-fn)]
;;     (start state-machine)
;;     (go
;;       (>! input-chan "2")
;;       (is (= "Got 2" (<! output-chan)))
;;       (<! (timeout (* timeout-ms 3))) ;; Wait for the timeout
;;       (is (= "Shutting down" (<! output-chan))))))

;; (deftest test-non-existent-state
;;   (let [input-chan (chan)
;;         state-map {:start (constantly :non-existent-state)}
;;         error-chan (chan)
;;         state-machine (make-state-machine
;;                        state-map
;;                        input-chan
;;                        :error-fn (fn [err]
;;                                    (go
;;                                      (>! error-chan err))))]
;;     (start state-machine)
;;     (go
;;       (>! input-chan "some input")
;;       (is (= "Next state (:non-existent-state) does not exist."
;;              (#+clj .getMessage #+cljs .-message
;;                     (<! error-chan)))))))

;; (deftest test-debug-fn
;;   (let [input-chan (chan)
;;         state-map {:start (fn [current-state input]
;;                             (case input
;;                               "2" :state2
;;                               "3" :state3
;;                               :done nil
;;                               nil))
;;                    :state2 (fn [current-state input]
;;                              (case input
;;                                "3" :state3
;;                                :done nil
;;                                nil))
;;                    :state3 (fn [current-state input]
;;                              (case input
;;                                :done nil
;;                                nil))
;;                    :unexpected (fn [current-state input])}
;;         debug-chan (chan 10)
;;         debug-fn (fn [msg]
;;                    (go
;;                      (>! debug-chan msg)))
;;         state-machine (make-state-machine state-map input-chan
;;                                           :debug-fn debug-fn)]
;;     (start state-machine)
;;     (go
;;       (is (= "Entering state :start" (<! debug-chan)))
;;       (>! input-chan "2")
;;       (is (= "Got input: 2" (<! debug-chan)))
;;       (is (= "Next state is :state2" (<! debug-chan)))
;;       (is (= "Entering state :state2" (<! debug-chan)))
;;       (>! input-chan "3")
;;       (is (= "Got input: 3" (<! debug-chan)))
;;       (is (= "Next state is :state3" (<! debug-chan)))
;;       (is (= "Entering state :state3" (<! debug-chan)))
;;       (>! input-chan :done)
;;       (is (= "Got input: :done" (<! debug-chan)))
;;       (is (= "Next state is nil" (<! debug-chan))))))
