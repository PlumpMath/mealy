(defproject farbetter/mealy "0.7.3"
  :description "State machine using core.async chans and running in a go block"
  :url "https://github.com/farbetter/mealy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[com.cemerick/clojurescript.test "0.3.1"]
            [lein-cljsbuild "1.0.3"]
            [lein-release "1.0.5"]]

  :dependencies
  [[com.taoensso/encore "1.18.3"]
   [com.taoensso/timbre "3.3.1"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/clojurescript "0.0-2411"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :profiles {:dev
             {:plugins [[com.keminglabs/cljx "0.4.0"]]
              :repl-options
              {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl
                                  cljx.repl-middleware/wrap-cljx]}}}

  :hooks [cljx.hooks leiningen.cljsbuild]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/clj"
                   :rules :clj}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/cljs"
                   :rules :cljs}]}

  :cljsbuild
  {:builds
   {"code" {:source-paths ["target/generated/src/cljs"]
            :compiler {:output-to "target/main.js"
                       :optimizations :simple
                       :pretty-print false
                       :jar true}}
    "test" {:source-paths ["target/generated/src/cljs"
                           "target/generated/test/cljs"]
            :compiler {:output-to "target/unit-test.js"
                       :optimizations :simple
                       :warnings true
                       :pretty-print false}}}
   :test-commands {"unit-tests" ["phantomjs" :runner
                                 "target/unit-test.js"]}}

  :deploy-repositories [["clojars" {:creds :gpg}]]  
  :lein-release {:scm {:name "git"
                       :url "https://github.com/farbetter/mealy"}
                 :deploy-via :clojars}

  

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :source-paths ["target/generated/src/clj" "src/clj"]
  :resource-paths ["target/generated/src/cljs"]
  :test-paths ["target/generated/test/clj"]  

  :aliases
  {"testclj" ["do" "clean," "cljx" "once," "test"]
   "testcljs" ["do" "clean," "cljx" "once," "cljsbuild" "test"]
   "testall" ["do" "testclj," "testcljs"]})
