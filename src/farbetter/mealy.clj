(ns farbetter.mealy
  (:require
   [clojure.core.async
    :refer [alts! chan dropping-buffer alts! go timeout <! >!]]))

(defn- exception->ex-info [e]
  (if (instance? clojure.lang.ExceptionInfo e)
    e
    (ex-info (.getMessage e) {:type :state-machine-exception
                              :original-exception e})))

(defn run-state-machine
  "See README.md for explanation of parameters"
  [state-map input-chan 
   & {:keys [error-chan timeout-ms timeout-fn shutdown-fn]
      :or {error-chan (chan (dropping-buffer 10))}}]
  (go
    (try
      (let [state (atom :start)]
        (while @state
          (let [state-fn (state-map @state)
                timeout-chan (when timeout-ms
                               (timeout timeout-ms))
                [data ch] (if timeout-ms
                            (alts! [input-chan timeout-chan])
                            [(<! input-chan) input-chan])
                next-state (if (nil? data)
                             (condp = ch
                               input-chan nil                ;; Chan closed
                               timeout-chan (when timeout-fn ;; Chan timeout
                                              (timeout-fn)))
                             (when state-fn
                               (state-fn @state data)))]
            (when next-state
              (when-not (contains? state-map next-state)
                (throw (IllegalArgumentException.
                        (str "Next state (" next-state ") does not exist.")))))
            (reset! state next-state)))
        (when shutdown-fn
          (shutdown-fn)))
      (catch Exception e (>! error-chan (exception->ex-info e)))))
  error-chan)
