# mealy
=======

A Clojure state machine using core.async chans and running in a go block


##Installation via Leiningen / Clojars:

[![Clojars Project](http://clojars.org/farbetter/mealy/latest-version.svg)](http://clojars.org/farbetter/mealy)

##Usage:
```Clojure
(ns com.example.your-application
  (:require [farbetter.mealy :refer [make-state-machine start stop]]))
```

Create a state machine by calling `make-state-machine`:
```Clojure
(def state-machine (make-state-machine state-map input-chan
                    :debug-fn println))
```
  
###Parameters:
Required Parameters:
 - `state-map` - Map of state keywords -> state functions
 - `input-chan` - A core.async channel for receiving input

Optional parameters passed in as :key val pairs 
      - `:timeout-ms` - ms to wait for input before timing out. If no
            `:timeout-fn` is specified, the state machine will exit on timeout.
      - `:timeout-fn` - Function to be called when timeouts occur.  If you 
            specify `:timeout-fn`, you must also specify `:timeout-ms`. This
            function should take the current state as an argument and should
            return either the name of the next state or nil to exit.
      - `:shutdown-fn` - Function to be called when the state machine exits.
                       This function should take no arguments. 
      - `:error-fn` - Function to be called when there are errors in the
            state machine. The specified function will be called with one
            argument, an ExceptionInfo object.
      - `:debug-fn` - Function of one argument to receive debugging calls.
            The specified function will be called at various points during
            the state machine's excecution to give debugging information.
            The function should accept one string argument.
            
The state-map must include a `:start` key, which is the state machine's
initial state. All values in the state map should be functions of two
arguments: `[current-state input]`.


To start or stop the state machine, use the appropriate methods:
```Clojure
(start state-machine)
(stop state-machine)
```


Once the state machine is started, **nothing happens until an input is
received on the `input-chan` or until a user-specified timeout is reached.**


When input is received, the appropriate state function is looked up in the
`state-map` parameter using the current state name as a key. That state
function is then called with the arguments `[current-state input]`. The state 
function should return the name of the next state or nil to exit the state
machine. The state machine will then move into the state returned by the state
function.


###Usage examples:
These examples are from the test suite and are presented as clojure.test tests.

```Clojure
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
```
