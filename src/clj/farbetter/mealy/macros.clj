(ns farbetter.mealy.macros)


(defmacro <!!? [ch]
  `(farbetter.mealy/throw-err (~'<!! ~ch)))

(defmacro <!? [ch]
  `(farbetter.mealy/throw-err (~'<! ~ch)))
