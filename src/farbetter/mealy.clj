(ns farbetter.mealy
  (:require
   [clojure.core.async
    :refer [alts! chan close! go timeout <! <!! >!]]))

(declare exception->ex-info)

(defprotocol Lifecycle
  (start [this])
  (stop [this]))

(defn- get-next-state [state-machine data ch timeout-chan]
  (let [{:keys [state input-chan debug-fn timeout-fn state-map]} state-machine
        state-fn (state-map @state)]
    (if (nil? data)
      (condp = ch
        input-chan (do
                     (when debug-fn
                       (debug-fn "Input channel closed."))
                     nil) ;; Exit state machine
        timeout-chan (do
                       (when debug-fn
                         (debug-fn "Timeout waiting for input."))
                       (when timeout-fn ;; Chan timeout
                         (timeout-fn @state))))
      (do
        (when debug-fn
          (debug-fn (str "Got input: " data)))
        (when state-fn
          (state-fn @state data))))))

(defrecord StateMachine [state state-map input-chan timeout-ms timeout-fn shutdown-fn
                         error-fn debug-fn]
  Lifecycle
  (start [this]
    (go
      (try
        (while @state
          (when debug-fn
            (debug-fn (str "Entering state " @state)))
          (let [timeout-chan (when timeout-ms
                               (timeout timeout-ms))
                [data ch] (if timeout-ms
                            (alts! [input-chan timeout-chan])
                            [(<! input-chan) input-chan])
                next-state (get-next-state this data ch timeout-chan)]
            (when debug-fn
              (debug-fn (str "Next state is " (if (nil? next-state)
                                                "nil"
                                                next-state))))
            (when next-state
              (when-not (contains? state-map next-state)
                (throw (IllegalArgumentException.
                        (str "Next state (" next-state ") does not exist.")))))
            (reset! state next-state)))
        (when shutdown-fn
          (shutdown-fn))
        (catch Exception e (when error-fn
                             (error-fn (exception->ex-info e)))))))
  (stop [this]
    (close! input-chan)))

(defn make-state-machine
  "See README.md for explanation of parameters"
  [state-map input-chan &
   {:keys [timeout-ms timeout-fn shutdown-fn error-fn debug-fn]}]
  (->StateMachine (atom :start) state-map input-chan timeout-ms timeout-fn
                  shutdown-fn error-fn debug-fn))

(defn- exception->ex-info [e]
  (if (instance? clojure.lang.ExceptionInfo e)
    e
    (ex-info (.getMessage e) {:type :state-machine-exception
                              :original-exception e})))

(defn throw-err [e]
  (when (instance? Throwable e)
    (throw e))
  e)

(defmacro <!!? [ch]
  `(farbetter.mealy/throw-err (<!! ~ch)))

(defmacro <!? [ch]
  `(farbetter.mealy/throw-err (<! ~ch)))
