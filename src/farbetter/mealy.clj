(ns farbetter.mealy
  (:require
   [clojure.core.async :refer [<!! alts! go timeout]]))



(defn run-state-machine
  "See README.md for explanation of parameters"
  [state-map input-chan & {:keys [timeout-ms timeout-fn shutdown-fn]}]
  (go
    (let [state (atom :start)]
      (while @state
        (let [state-fn (state-map @state)
              timeout-chan (when timeout-ms
                             (timeout timeout-ms))
              [data ch] (if timeout-ms
                          (alts! [input-chan timeout-chan])
                          [(<!! input-chan) input-chan])
              next-state (if (nil? data)
                           (condp = ch
                             input-chan nil                ;; Chan closed
                             timeout-chan (when timeout-fn ;; Chan timeout
                                            (timeout-fn)))
                           (when state-fn
                             (state-fn @state data)))]
          (reset! state next-state)))
      (when shutdown-fn
        (shutdown-fn)))))


