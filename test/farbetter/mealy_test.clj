(ns farbetter.mealy-test
  (:require
   [clojure.core.async :refer [>!! <!! chan]]
   [clojure.test :refer :all]
   [farbetter.mealy :refer [make-state-machine start]]))


(deftest test-basics
  (let [input-chan (chan)
        output-chan (chan)
        state-map {:start (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "2" :state2
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state2 (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state3 (fn [current-state input]
                             (>!! output-chan (str "Got " input))
                             (case input
                               :done nil
                               :unexpected))
                   :unexpected (fn [current-state input]
                                 (>!! output-chan "Got unexpected input"))}
        state-machine (make-state-machine state-map input-chan)]
    (start state-machine)
    (>!! input-chan "2")
    (is (= "Got 2" (<!! output-chan)))
    (>!! input-chan "3")
    (is (=  "Got 3"(<!! output-chan)))
    (>!! input-chan :done)
    (is (= "Got :done" (<!! output-chan)))))

(deftest test-timeout
  (let [input-chan (chan)
        output-chan (chan)
        state-map {:start (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "2" :state2
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state2 (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state3 (fn [current-state input]
                             (>!! output-chan (str "Got " input))
                             (case input
                               :done nil
                               :unexpected))
                   :unexpected (fn [current-state input]
                                 (>!! output-chan "Got unexpected input"))}
        timeout-ms 10
        tof (fn [current-state]
             (>!! output-chan "TIMEOUT!!!!!!!!")
             nil)
        state-machine (make-state-machine state-map input-chan
                                          :timeout-ms timeout-ms
                                          :timeout-fn tof)]
    (start state-machine)
    (>!! input-chan "2")
    (is (= "Got 2" (<!! output-chan)))
    (Thread/sleep (* timeout-ms 3)) ;; Wait for the timeout
    (is (=  "TIMEOUT!!!!!!!!"(<!! output-chan)))))

(deftest test-shutdown-fn
  (let [input-chan (chan)
        output-chan (chan)
        state-map {:start (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "2" :state2
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state2 (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state3 (fn [current-state input]
                             (>!! output-chan (str "Got " input))
                             (case input
                               :done nil
                               :unexpected))
                   :unexpected (fn [current-state input]
                                 (>!! output-chan "Got unexpected input"))}
        shutdown-fn (fn []
                      (>!! output-chan "Shutting down"))
        state-machine (make-state-machine state-map input-chan
                                          :shutdown-fn shutdown-fn)]
    (start state-machine)
    (>!! input-chan "2")
    (is (= "Got 2" (<!! output-chan)))
    (>!! input-chan :done)
    (is (= "Got :done" (<!! output-chan)))
    (is (= "Shutting down" (<!! output-chan)))))

(deftest test-timeout-w-no-timeout-fn
  (let [input-chan (chan)
        output-chan (chan)
        state-map {:start (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "2" :state2
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state2 (fn [current-state input]
                            (>!! output-chan (str "Got " input))
                            (case input
                              "3" :state3
                              :done nil
                              :unexpected))
                   :state3 (fn [current-state input]
                             (>!! output-chan (str "Got " input))
                             (case input
                               :done nil
                               :unexpected))
                   :unexpected (fn [current-state input]
                                 (>!! output-chan "Got unexpected input"))}
        timeout-ms 10
        shutdown-fn (fn []
                      (>!! output-chan "Shutting down"))
        state-machine (make-state-machine state-map input-chan
                                         :timeout-ms timeout-ms
                                         :shutdown-fn shutdown-fn)]
    (start state-machine)
    (>!! input-chan "2")
    (is (= "Got 2" (<!! output-chan)))
    (Thread/sleep (* timeout-ms 3)) ;; Wait for the timeout
    (is (= "Shutting down" (<!! output-chan)))))

(deftest test-non-existent-state
  (let [input-chan (chan)
        state-map {:start (constantly :non-existent-state)}
        error-chan (chan)
        state-machine (make-state-machine state-map input-chan
                                          :error-fn #(>!! error-chan %))]
    (start state-machine)
    (>!! input-chan "some input")
    (is (= "Next state (:non-existent-state) does not exist."
           (.getMessage (<!! error-chan))))))

(deftest test-debug-fn
  (let [input-chan (chan)
        state-map {:start (fn [current-state input]
                            (case input
                              "2" :state2
                              "3" :state3
                              :done nil
                              nil))
                   :state2 (fn [current-state input]
                            (case input
                              "3" :state3
                              :done nil
                              nil))
                   :state3 (fn [current-state input]
                             (case input
                               :done nil
                               nil))
                   :unexpected (fn [current-state input])}
        debug-chan (chan 10)
        debug-fn #(>!! debug-chan %)
        state-machine (make-state-machine state-map input-chan
                                          :debug-fn debug-fn)]
    (start state-machine)
    (is (= "Entering state :start" (<!! debug-chan)))
    (>!! input-chan "2")
    (is (= "Got input: 2" (<!! debug-chan)))
    (is (= "Next state is :state2" (<!! debug-chan)))
    (is (= "Entering state :state2" (<!! debug-chan)))
    (>!! input-chan "3")
    (is (= "Got input: 3" (<!! debug-chan)))
    (is (= "Next state is :state3" (<!! debug-chan)))
    (is (= "Entering state :state3" (<!! debug-chan)))
    (>!! input-chan :done)
    (is (= "Got input: :done" (<!! debug-chan)))
    (is (= "Next state is nil" (<!! debug-chan)))))
