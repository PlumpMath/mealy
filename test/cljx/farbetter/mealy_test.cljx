(ns farbetter.mealy-test
  (:require
   #+clj  [cemerick.cljs.test :refer [block-or-done]]
   #+cljs [cemerick.cljs.test :as t]
   #+cljs [cljs.core.async :refer [>! <! chan timeout]]
   #+clj  [clojure.core.async :refer [>! <! chan go timeout]]
   #+clj  [clojure.test :refer [deftest is]]
   [farbetter.mealy :refer [make-state-machine start]]
   #+clj  [farbetter.mealy.macros :refer [<!? <!!?]]
   #+clj [taoensso.timbre :as timbre :refer [debug error info]]
   #+cljs [taoensso.encore :refer [debugf errorf infof warnf]])
  #+cljs (:require-macros
          [cemerick.cljs.test :refer [block-or-done deftest is]]
          [cljs.core.async.macros :refer [go]]
          [farbetter.mealy.macros :refer [<!?]]))

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


(deftest ^:async test-basics
  (let [input-chan (chan)
        output-chan (chan)
        control-chan (chan)
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan)]
    (start state-machine)
    (go
      (>! input-chan "2")
      (is (= "Got 2" (<! output-chan)))
      (>! input-chan "3")
      (is (=  "Got 3" (<! output-chan)))
      (>! input-chan :done)
      (is (= "Got :done" (<! output-chan)))
      (>! control-chan true))
    (block-or-done control-chan)))

(deftest ^:async test-timeout
  (let [input-chan (chan)
        output-chan (chan)
        control-chan (chan)
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
    (go
      (>! input-chan "2")
      (is (= "Got 2" (<! output-chan)))
      (<! (timeout (* timeout-ms 3))) ;; Wait for the timeout
      (is (=  "TIMEOUT!!!!!!!!"(<! output-chan)))
      (>! control-chan true))
    (block-or-done control-chan)))

(deftest ^:async test-shutdown-fn
  (let [input-chan (chan)
        output-chan (chan)
        shutdown-chan (chan)
        control-chan (chan)        
        shutdown-fn (fn []
                      (go
                        (>! shutdown-chan "Shutting down")))
        state-machine (make-state-machine
                       (make-state-map output-chan) input-chan
                       :shutdown-fn shutdown-fn)]
    (start state-machine)
    (go
      (>! input-chan :done)
      (is (= "Shutting down" (<! shutdown-chan)))
      (>! control-chan true))
    (block-or-done control-chan)))


(deftest ^:async test-timeout-w-no-timeout-fn
  (let [input-chan (chan)
        output-chan (chan)
        control-chan (chan)        
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
      (is (= "Shutting down" (<! output-chan)))
      (>! control-chan true))
    (block-or-done control-chan)))

(deftest ^:async test-non-existent-state
  (let [input-chan (chan)
        state-map {:start (constantly :non-existent-state)}
        error-chan (chan)
        control-chan (chan)
        state-machine (make-state-machine
                       state-map
                       input-chan
                       :error-fn (fn [err]
                                   (go
                                     (>! error-chan err))))]
    (start state-machine)
    (go
      (>! input-chan "some input")
      (is (= (ex-data (<! error-chan))
             {:type :nonexistent-next-state,
              :next-state :non-existent-state}))
      (>! control-chan true))
    (block-or-done control-chan)))

(deftest ^:async test-<!!?
  (let [control-chan (chan)
        test-chan (chan 1)
        ex (ex-info "test" {:type :test})]
    (go
      (>! test-chan ex)
      (is (thrown-with-msg? #+clj clojure.lang.ExceptionInfo
                            #+cljs cljs.core/ExceptionInfo
                            #"test"
                            (<!? test-chan)))
      (>! control-chan true))
    (block-or-done control-chan)))

;; TODO: Fix ordering issue in debug fn. Make debug msg order
;; determinisitc.

;; (deftest ^:async test-debug-fn
;;   (let [input-chan (chan)
;;         output-chan (chan 10)
;;         debug-chan (chan 10)
;;         control-chan (chan)
;;         state-map (make-state-map output-chan)
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
;;       ;;      (is (= "Got input: :done" (<! debug-chan)))
;;       ;;      (is (= "Next state is nil" (<! debug-chan)))
;;       (>! control-chan true))
;;     (block-or-done control-chan)))

