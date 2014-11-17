(ns farbetter.mealy
  (:require
   [#+clj clojure.core.async
    #+cljs cljs.core.async
    :refer [alts! chan close! timeout <! #+clj <!! >! #+clj go]])
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]]))

#+cljs (def Exception js/Error)
#+cljs (def Throwable js/Error)

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
                (throw (ex-info 
                        (str "Next state (" next-state ") does not exist.")
                        {:type :nonexistent-next-state
                         :next-state next-state}))))
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
  (if (instance? #+clj clojure.lang.ExceptionInfo
                 #+cljs cljs.core/ExceptionInfo
                 e)
    e
    (ex-info (.getMessage e) {:type :state-machine-exception
                              :original-exception e})))

(defn throw-err [e]
  (when (instance? Throwable e)
    (throw e))
  e)

