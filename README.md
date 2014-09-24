# mealy
=======

##### A Clojure state machine using core.async chans and running in a go block

There is only one core function:
```Clojure
(defn run-state-machine
  [state-map input-chan & {:keys [timeout-ms timeout-fn shutdown-fn] :as opts}])
```
  
Parameters:
 - `state-map` - Map of state keywords -> state functions
 - `input-chan` - A core.async channel for receiving input
 - `opts` (optional) Passed in as :key val pairs 
      - `:timeout-ms` - ms to wait for input before timing out. If no
            `:timeout-fn` is specified, the state machine will exit on timeout.
      - `:timeout-fn` - Function to be called when timeouts occur.  If you 
            specify `:timeout-fn`, you must also specify `:timeout-ms`. This
            function should take no arguments and should return either the
            name of the next state or nil to exit.
      - :`shutdown-fn` - Function to be called when the state machine exits.
                       This function should take no arguments. 
  
The state-map must include a `:start` key, which is the state machine's
initial state. All values in the state map should be functions of two
arguments: `[current-state input]`.


**Nothing happens until an input is received on the input
channel or until a user-specified timeout is reached.**


When input is received, the appropriate state function is looked up in the
`state-map` parameter using the current state name as a key. That state
function is then called with the arguments `[current-state input]`. The state 
function should return the name of the next state or nil to exit the state
machine. The state machine will then move into the state returned by the state
function.


If `:timeout-ms` is specified, timeouts are enabled. If the specifed amount of
time passes while waiting for input, the function specified by `:timeout-fn`
is called with no arguments. The timeout-fn should return the name of the next
state or nil to exit the state machine. If no `:timeout-fn` is specified, the
state machine will exit on timeout.


If a `:shutdown-fn` is specified, the given function will be called with no
arguments when the state machine exits.


Usage examples:
These examples are from the test suite and are presented as clojure.test tests.

```Clojure
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
        tof (fn []
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
```
