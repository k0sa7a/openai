(ns openai.openai-connector
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [environ.core :refer [env]]))

;; Dont forget to export the env variable in the terminal
;;  export OPENAI_API_KEY=xxxxxxx
;; You then can use the api key
(def openai-api-key (env :openai-api-key))


;; Test on the cheshire library
;; (json/generate-string {:foo "bar" :baz 5})

;; Test to see if access to the API is OK
;; (client/get "https://api.openai.com/v1/models"
;;             {:headers {"Authorization" (str "Bearer " openai-api-key)}})

;; If we want to print just the body
;; (let [response (client/get "https://api.openai.com/v1/models"
;;                            {:headers {"Authorization" (str "Bearer " openai-api-key)}})
;;       body (:body response)]
;;   (println body))
