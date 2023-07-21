(ns rest-api.core
  (:require [org.httpkit.server :as server]
            [compojure.route :as route]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
    (:gen-class))

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
(adduser "Jane" "Smith", "Oulu")
(adduser "John" "Doe", "Helsinki")

; Return List of Users
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
                        (adduser (p :firstname) (p :surname) (p :city))
                        (str (json/write-str @users-collection))))})


(defn update-city [req]
  (let [id (-> req :params :id)
        city (-> req :params :city)]
    (swap! users-collection
           (fn [users]
             (mapv (fn [user]
                     (if (= (:id user) (Integer. id))
                       (assoc user :city city)
                       user))
                   users)))
    {:status 200
     :headers {"Content-Type" "text/json"}
     :body (json/write-str @users-collection)}))

(defn delete-user [req]
  (let [id (-> req :params :id)]
    (swap! users-collection
           (fn [users]
             (->> users
                  (filter #(not= (:id %) (Integer. id)))
                  vec)))
    {:status 200
     :headers {"Content-Type" "text/json"}
     :body (json/write-str @users-collection)}))

(defroutes app-routes
  (GET "/users" [] user-handler)
  (POST "/users/add" [] adduser-handler)
  (PUT "/user/update" [] update-city)
  (DELETE "/user/delete" [] delete-user)
  (route/not-found "Page not found!"))

(defn -main
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4000"))]
      ; Run the server with Ring.defaults middleware
    (server/run-server
     (-> app-routes
         (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false)))
     {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))


