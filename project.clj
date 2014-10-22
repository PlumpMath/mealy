(defproject farbetter/mealy "0.6.0"
  :description "State machine using core.async chans and running in a go block"
  :url "https://github.com/farbetter/mealy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-release "1.0.5"]]
  :lein-release {:scm {:name "git"
                       :url "https://github.com/farbetter/mealy"}
                 :deploy-via :clojars}
  :deploy-repositories [["clojars" {:creds :gpg}]]  
  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]])
