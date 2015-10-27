(defproject farbetter/mealy "0.7.11"
  :description "State machine using core.async chans and running in a go block"
  :url "https://github.com/farbetter/mealy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :lein-release {:scm :git
                 :deploy-via :clojars}
  :deploy-repositories [["clojars" {:creds :gpg}]]

  :profiles {:dev
             {:plugins [[lein-cljsbuild "1.1.0"]
                        [lein-release "1.0.9"]]}}

  :dependencies
  [[com.taoensso/timbre "4.1.4"]
   [farbetter/utils "0.1.22"]
   [org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.145"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :repl-options
  {:init-ns farbetter.mealy}

  :cljsbuild
  {:builds
   [{:id "node-dev"
     :source-paths ["src" "test"]
     :notify-command ["node" "target/test/node_dev/test_main.js"]
     :compiler
     {:optimizations :none
      :main "farbetter.test-runner"
      :target :nodejs
      :output-to "target/test/node_dev/test_main.js"
      :output-dir "target/test/node_dev"
      :source-map true}}]}

  :aliases
  {"auto-test-cljs" ["do"
                     "clean,"
                     "cljsbuild" "auto" "node-dev"]})
