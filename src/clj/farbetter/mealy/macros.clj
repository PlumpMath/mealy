(ns farbetter.mealy.macros)

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro throw-err [e]
  `(let [e# ~e]
     (if (instance? Throwable e#)
       (throw e#)
       e#)))

(defn throw-err [e]
  (if (instance? Throwable e)
    (throw e)
    e))

(defmacro <!? [ch]
  `(if-cljs
    (throw-err (cljs.core.async/<! ~ch))
    (throw-err (clojure.core.async/<! ~ch))))

(defmacro <!!? [ch]
  `(if-cljs
    (throw-err (cljs.core.async/<!! ~ch))
    (throw-err (clojure.core.async/<!! ~ch))))
