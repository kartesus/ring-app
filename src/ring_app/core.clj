(ns ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.http-response :as response]
            [ring.middleware.reload :refer [wrap-reload]]
            [muuntaja.middleware :refer [wrap-format]]
            [reitit.ring :as reitit]))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
        (assoc-in [:headers "Pragma"] "no-cache")
        (assoc-in [:headers "Expires"] "0"))))

(defn html-handler [{{:keys [name]} :path-params}]
  (response/ok (str "<h1>Hello, " name "!</h1>")))

(defn json-handler [request]
  (response/ok {:message (str "Hello, " (get-in request [:body-params :name]) "!")}))

(def routes
  [["/hello/:name" {:get html-handler}]
   ["/api" {:middleware [wrap-format]
            :post json-handler}]])

(def handler
  (reitit/ring-handler
   (reitit/router routes)
   (reitit/routes
    (reitit/create-resource-handler {:path "/"})
    (reitit/create-default-handler {:not-found
                                    (constantly (response/not-found "404 - Page not found"))
                                    :method-not-allowed
                                    (constantly (response/method-not-allowed "405 - Method not allowed"))
                                    :not-acceptable
                                    (constantly (response/not-acceptable "406 - Not acceptable"))}))))

(defn -main []
  (jetty/run-jetty (-> #'handler
                       wrap-nocache
                       wrap-reload)
                   {:port 8080 :join? false}))