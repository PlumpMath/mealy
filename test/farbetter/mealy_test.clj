(ns farbetter.mealy-test
  (:require
   [clojure.core.async :refer [>!! <!! chan]]
   [clojure.test :refer :all]
   [farbetter.mealy :refer :all]))


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
                                 (>!! output-chan "Got unexpected input"))}]
    (run-state-machine state-map input-chan)
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
             nil)]
    (run-state-machine state-map input-chan
                       :timeout-ms timeout-ms
                       :timeout-fn tof)
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
                      (>!! output-chan "Shutting down"))]
    (run-state-machine state-map input-chan
                       :shutdown-fn shutdown-fn)
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
                      (>!! output-chan "Shutting down"))]
    (run-state-machine state-map input-chan
                       :timeout-ms timeout-ms
                       :shutdown-fn shutdown-fn)
    (>!! input-chan "2")
    (is (= "Got 2" (<!! output-chan)))
    (Thread/sleep (* timeout-ms 3)) ;; Wait for the timeout
    (is (= "Shutting down" (<!! output-chan)))))

(deftest test-non-existent-state-using-returned-error-chan
  (let [input-chan (chan)
        state-map {:start (constantly :non-existent-state)}
        error-chan (run-state-machine state-map input-chan)]
    (>!! input-chan "some input")
    (is (= "Next state (:non-existent-state) does not exist."
           (.getMessage (<!! error-chan))))))

(deftest test-non-existent-state-using-provided-error-chan
  (let [input-chan (chan)
        error-chan (chan)
        state-map {:start (constantly :non-existent-state)}]
    (run-state-machine state-map input-chan :error-chan error-chan)
    (>!! input-chan "some input")
    (is (= "Next state (:non-existent-state) does not exist."
           (.getMessage (<!! error-chan))))))
