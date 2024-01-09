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




;; function to create a thread (no messages created for it)
;; it returns the id of that thread
(defn create-thread []
  (let [response (client/post "https://api.openai.com/v1/threads" {:headers {"Authorization" (str "Bearer " openai-api-key)
                                                                             "Content-Type" "application/json"
                                                                             "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))



;; function to create a single message
;; returns the id an message content
(defn create-message [content thread-id]
  (let [payload (json/generate-string {"role" "user"
                                       "content" content})
        response (client/post (str "https://api.openai.com/v1/threads/" thread-id "/messages")
                              {:body  payload
                               :headers {"Authorization" (str "Bearer " openai-api-key)
                                         "Content-Type" "application/json"
                                         "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]
    (println parsed-body)
    (println "---------------------------")
    (get parsed-body :id)))

;; old test with hard coded strings for the messages
;; (create-message "1 - Feefo")
;; (create-message "2 - MySQL")
;; (create-message "3 - NetSuite")
;; (create-message "42 - Mailchimp")


;; create a run
;; returns a run id
(defn create-run [assistant-id thread-id]
  (let [payload (json/generate-string {"assistant_id" assistant-id})
        response (client/post (str "https://api.openai.com/v1/threads/" thread-id "/runs")
                              {:body  payload
                               :headers {"Authorization" (str "Bearer " openai-api-key)
                                         "Content-Type" "application/json"
                                         "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (get parsed-body :id)))



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

(defn list-run-steps [thread-id run-id]
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

(defn retrieve-message [thread-id msg-id]
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





;; delete the thread after run is done; prints body to check if deleted true
(defn delete-thread [thread-id]
  (let [response (client/delete (str "https://api.openai.com/v1/threads/" thread-id)
                                {:headers {"Authorization" (str "Bearer " openai-api-key)
                                           "Content-Type" "application/json"
                                           "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (println parsed-body)))



;; delete the assistant after run is done; prints body to check if deleted true
(defn delete-assistant [assistant-id]
  (let [response (client/delete (str "https://api.openai.com/v1/assistants/" assistant-id)  {
                                                                      :headers {"Authorization" (str "Bearer " openai-api-key)
                                                                                "Content-Type" "application/json"
                                                                                "OpenAI-Beta" "assistants=v1"}})
        body (:body response)
        parsed-body (json/parse-string body true)]

    (println parsed-body)))



;; parse the csv into a sequence of maps
(defn parse-csv [file-path]
  (with-open [reader (io/reader file-path)]
    (let [rows (csv/read-csv reader)
          headers (map keyword (first rows))]
      (doall (map (fn [row] (zipmap headers row)) (rest rows))))))




;; extract only the required columns and prepare sequence of strings to be used in message creation
(defn extract-and-concat [data key1 key2]
  (map #(str ((keyword key1) %) " - " ((keyword key2) %)) data))



;; --- FLOW FOR THE OPEN AI TEST ---

;; ;; OPTION 1 (many messages and one run)..........|
;; create an assistant
;; (def assistant-id (create-assistant "Assistant test 2" "gpt-3.5-turbo-1106" "Please return the ID, company name and generate your own description of the value for each in the format: ID - Value - Description; Do not return anything else"))
;; (println "Assistant ID is:" assistant-id)

;; ;; create a thread
;; (def thread-id (create-thread))
;; (println "Thread ID is:" thread-id)

;; ;; parse the csv with initial data
;; (def data (parse-csv "src/openai/data.csv"))

;; ;; extract the columns that we require and concat the data
;; (def sequence-for-messages (extract-and-concat data :FEATURE_ID :FEATURE_NAME))
;; (prn sequence-for-messages)


;; ;; create the messages
;; (doseq [msg sequence-for-messages]
;;   (create-message msg thread-id)
;;   (Thread/sleep 1000))

;; ;; create the run
;; (def run-id (create-run assistant-id thread-id))
;; (println "Run ID is:" run-id)

;; ;; list run steps
;; (def msg-id (list-run-steps thread-id run-id))
;; (println "Message ID is:" msg-id)

;; ;; retrieve the message
;; (println (retrieve-message thread-id msg-id))

;; ;; delete thread
;; (delete-thread thread-id)

;; ;; delete assistant
;; (delete-assistant assistant-id)
;; ;; ...............................................|


;; OPTON 1 (re-writing)==========}
(let [
      ;; create an assistant
      assistant-id (create-assistant "Assistant test 2" "gpt-3.5-turbo-1106" "Please return the ID, company name and generate your own description of the value for each in the format: ID - Value - Description; Do not return anything else")
      ;; create a thread
      thread-id (create-thread)
      ;; parse the csv with initial data
      data (parse-csv "src/openai/data.csv")
      ;; extract the columns that we require and concat the data
      sequence-for-messages (extract-and-concat data :FEATURE_ID :FEATURE_NAME)]


  (println "Assistant ID is:" assistant-id)
  (println "Thread ID is:" thread-id)

  ;; create the messages
  (doseq [msg sequence-for-messages]
    (create-message msg thread-id)
    (Thread/sleep 1000))

  (let [
        ;; create the run
        run-id (create-run assistant-id thread-id)
        ;; list run steps
        msg-id (list-run-steps thread-id run-id)]
    (println "Run ID is:" run-id)
    (println "Message ID is:" msg-id)
    ;; retrieve the message
    (println (retrieve-message thread-id msg-id)))

  ;; delete thread
  (delete-thread thread-id)
  ;; delete assistant
  (delete-assistant assistant-id)
  )

;; ============================}
