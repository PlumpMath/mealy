(defproject farbetter/mealy "0.1.0-SNAPSHOT"
  :description "State machine using core.async chans and running in a go block"
  :url "http://www.farbetter.com"
  :license "Eclipse Public License - v 1.0"
  :plugins [[lein-release "1.0.5"]]
  :lein-release {:scm :git
                 :deploy-via :clojars}
  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [org.clojure/core.async "0.1.338.0-5c5012-alpha"]])
