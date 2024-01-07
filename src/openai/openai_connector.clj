(ns openai.openai-connector
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            ))

;; First export the env variable in the terminal
;; export OPENAI_API_KEY=xxxxxxx
;; You then can use the api key
(def openai-api-key (env :openai-api-key))


;; list the models to choose from
(defn list-models []
  (let [response (client/get "https://api.openai.com/v1/models"
                             {:headers {"Authorization" (str "Bearer " openai-api-key)}})
        body (:body response)]
    (println body))
  )
;; (list-models)


;; function to create an assistant
;; it returns the id of that assistant
(defn create-assistant [name model instruction]
  (let [payload (json/generate-string {"instructions" instruction
                                       "name" name
                                      ;;  "tools" [{"type" "retrieval"}]
                                       "model" model})
        response (client/post "https://api.openai.com/v1/assistants" {:body  payload
                                                                      :headers {"Authorization" (str "Bearer " openai-api-key)
                                                                                "Content-Type" "application/json"
                                                                                "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))


(def assistant-id (create-assistant "Assistant test1" "gpt-3.5-turbo-1106" "You will reply in rhymes"))
(println "Assistant ID is:" assistant-id)

;; function to create a thread (no messages created for it)
;; it returns the id of that thread
(defn create-thread []
  (let [response (client/post "https://api.openai.com/v1/threads" {:headers {"Authorization" (str "Bearer " openai-api-key)
                                                                             "Content-Type" "application/json"
                                                                             "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))

(def thread-id (create-thread))
(println "Thread ID is:" thread-id)
