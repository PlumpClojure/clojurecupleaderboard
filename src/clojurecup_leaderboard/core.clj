(ns clojurecup-leaderboard.core
  (:require [cognitect.transit :as transit]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            ))

(def cached (atom [nil nil]))

(defn expired? [timestamp]
  (< timestamp (- (System/currentTimeMillis) 3600000)))

(defn fetch-leaderboard []
  (let [[timestamp data] @cached]
    (if (or (nil? timestamp) (nil? data) (expired? timestamp))
      (reset! cached [(System/currentTimeMillis)
                      (-> "https://backend.clojurecup.com/teams"
                          (io/input-stream)
                          (transit/reader  :json)
                          (transit/read))])

      (last @cached))))

(fetch-leaderboard)

(defn leaderboard []
  (->> (fetch-leaderboard)
       (sort-by #(* -1 (or (:faver-count %) 0)))
       (map #(hash-map :place (inc %1)
                       :app (:team/app-name %2)
                       :url (str "https://clojurecup.com/#/apps/" (:team/app-domain %2))
                       :votes (:faver-count %2) ) (range))) )

(defn render-leaderboard []
  (html5
    [:head
     [:title "Clojure Cup 2014 Leaderboard"]
     [:link {:rel "stylesheet" :href "style.css"}]
     ]
    [:body
     [:h1 "Clojure Cup 2014 Leaderboard"]

     [:table
      [:thead
       [:tr
        [:th "Place"]
        [:th "App"]
        [:th "Votes"]]]
      [:tbody
       (for [{place :place app :app url :url votes :votes} (leaderboard)]
         [:tr
          [:td place]
          [:td
           [:a {:href url} app]]
          [:td votes]])]]
     [:footer
      "Courtesy of "
      [:a {:href "http://adambard.com/"} "Adam Bard"]
      ", whose app "
      [:a {:href "http://clojurecup.com/#/apps/booker"} "Booker"]
      " is not doing so great."]
     ]))

(defroutes app
  (GET "/" [] (render-leaderboard))
  (route/resources "/")
  (route/not-found "404")
  )

(defn -main []
  (run-jetty app {:port 8080}))
