(ns rest-api.core
  (:require [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET]]
            [compojure.core :refer [defroutes POST]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.defaults :refer [site-defaults]])
    (:gen-class))

; Simple Page
(defn simple-body-page [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "Hello World"})

(defn greeting-handler [req] ;(3)
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (->
             (pp/pprint req)
             (str "Hello " (:name (:params req))))})

(def users-collection (atom []))
(def id-counter (atom 0))

(defn adduser [firstname surname city]
  (swap! id-counter inc)
  (let [user {:id @id-counter
              :firstname (str/capitalize firstname)
              :surname (str/capitalize surname)
              :city city}]
    (swap! users-collection conj user)
    user))

; Example JSON objects
(adduser "Kalpani" "Ranasinghe", "Oulu")
(adduser "John" "Doe", "Helsinki")

; Return List of People
(defn user-handler [req]
        {:status  200
         :headers {"Content-Type" "text/json"}
         :body    (str (json/write-str @users-collection))})


; Helper to get the parameter specified by pname from :params object in req
(defn getparameter [req pname] (get (:params req) pname))

; Add a new person into the users-collection
(defn adduser-handler [req]
        {:status  200
         :headers {"Content-Type" "text/json"}
         :body    (-> (let [p (partial getparameter req)]
                        (str (json/write-str (adduser (p :firstname) (p :surname) (p :city))))))})


(defn update-city [id new-city]
  (swap! users-collection
         (fn [users]
           (mapv (fn [user]
                   (if (= (:id user) id)
                     (assoc user :city new-city)
                     user))
                 users)))
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (json/write-str @users-collection)})

(defn delete-user [id]
  (swap! users-collection
         (fn [users]
           (->> users
                (filter #(not= (:id %) id))
                vec)))
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (json/write-str @users-collection)})

(defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/greet" [] greeting-handler)
  (GET "/users" [] user-handler)
  (POST "/users/add" [] adduser-handler)
  (POST "/user/update" [id city] (update-city (Integer/parseInt id) city))
  (POST "/user/delete" [id] (delete-user (Integer/parseInt id)))
  (route/not-found "Page not found!"))

(defn -main
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4000"))]
      ; Run the server with Ring.defaults middleware
    (server/run-server
     (-> app-routes
         (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false)))
     {:port port})
      ; Run the server without ring defaults
      ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))


