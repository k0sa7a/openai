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


(def assistant-id (create-assistant "Assistant test1" "gpt-3.5-turbo-1106" "Please return the ID, company name and generate your own description of the value for each in the format: ID - Value - Description; Do not return anything else"))
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

;; function to create a single message
;; returns the id an message content
(defn create-message [content]
  (let [payload (json/generate-string {"role" "user"
                                       "content" content})
        response (client/post (str "https://api.openai.com/v1/threads/" thread-id "/messages")
                              {:body  payload
                               :headers {"Authorization" (str "Bearer " openai-api-key)
                                         "Content-Type" "application/json"
                                         "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))

(create-message "1 - Feefo")
(create-message "2 - MySQL")
(create-message "3 - NetSuite")
(create-message "42 - Mailchimp")


;; create a run
;; returns a run id
(defn create-run []
  (let [payload (json/generate-string {"assistant_id" assistant-id})
        response (client/post (str "https://api.openai.com/v1/threads/" thread-id "/runs")
                              {:body  payload
                               :headers {"Authorization" (str "Bearer " openai-api-key)
                                         "Content-Type" "application/json"
                                         "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))

(def run-id (create-run))
(println "Run ID is:" run-id)

;; list the run steps
;; (defn list-run-steps []
;;   (let [response (client/get (str "https://api.openai.com/v1/threads/" thread-id "/runs/" run-id "/steps")
;;                              {:headers {"Authorization" (str "Bearer " openai-api-key)
;;                                         "Content-Type" "application/json"
;;                                         "OpenAI-Beta" "assistants=v1"}})
;;         body (:body response)
;;         parsed-body (json/parse-string body true)]
;;     (println parsed-body)
;;     (get-in parsed-body [:data 0 :step_details :message_creation :message_id]))
;;   )

(defn list-run-steps []
  (loop []
    (let [response (client/get (str "https://api.openai.com/v1/threads/" thread-id "/runs/" run-id "/steps")
                               {:headers {"Authorization" (str "Bearer " openai-api-key)
                                          "Content-Type" "application/json"
                                          "OpenAI-Beta" "assistants=v1"}})
          body (:body response)
          parsed-body (json/parse-string body true)]
      (println parsed-body) ; For debugging purposes
      (if (empty? (:data parsed-body))
        (do
          (Thread/sleep 5000) ; Wait for 5 seconds before retrying
          (recur)) ; Retry the request
        (get-in parsed-body [:data 0 :step_details :message_creation :message_id])))))

(def msg-id (list-run-steps))
(println "Message ID is:" msg-id)
;; quick check on the run (to see if it failed)
;; (client/get (str "https://api.openai.com/v1/threads/" thread-id "/runs/" run-id)
;;             {:headers {"Authorization" (str "Bearer " openai-api-key)
;;                        "Content-Type" "application/json"
;;                        "OpenAI-Beta" "assistants=v1"}})


;; retrieve message
;; (defn retrieve-message []
;;   (let [response (client/get (str "https://api.openai.com/v1/threads/" thread-id "/messages/" msg-id)
;;                              {:headers {"Authorization" (str "Bearer " openai-api-key)
;;                                         "Content-Type" "application/json"
;;                                         "OpenAI-Beta" "assistants=v1"}})
;;         body (:body response)
;;         parsed-body (json/parse-string body true)]
;;    (get-in parsed-body [:content 0 :text :value])))

(defn retrieve-message []
  (loop []
    (let [response (client/get (str "https://api.openai.com/v1/threads/" thread-id "/messages/" msg-id)
                               {:headers {"Authorization" (str "Bearer " openai-api-key)
                                          "Content-Type" "application/json"
                                          "OpenAI-Beta" "assistants=v1"}})
          body (:body response)
          parsed-body (json/parse-string body true)
          value (get-in parsed-body [:content 0 :text :value])]
      ;; (println parsed-body) ; For debugging purposes
      (if (empty? value)
        (do
          (Thread/sleep 5000) ; Wait for 5 seconds before retrying
          (recur)) ; Retry the request
        value))))



(println (retrieve-message))
