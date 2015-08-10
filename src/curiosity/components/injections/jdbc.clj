;; This file must be required very early due to monkey patching
(ns curiosity.components.injections.jdbc
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [clj-time.coerce :as tc]
            ;; Require this to setup the json encoder
            curiosity.components.injections.json)
  (:import org.joda.time.DateTime
           org.postgresql.util.PGobject
           org.postgresql.jdbc4.Jdbc4Array))

;; setup JSON fields to automatically serialize-deserialize
(defn value-to-json-pgobject [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string value))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value] (value-to-json-pgobject value))

  clojure.lang.IPersistentVector
  (sql-value [value] (value-to-json-pgobject value))

  java.util.Date
  (sql-value [date]
    (tc/to-timestamp date))

  org.joda.time.DateTime
  (sql-value [date-time]
    (tc/to-timestamp date-time)))

(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.util.PGobject
  (result-set-read-column [pgobj metadata idx]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "jsonb" (json/parse-string value true)
        "json" (json/parse-string value true)
        value)))

  org.postgresql.jdbc4.Jdbc4Array
  (result-set-read-column [obj result-meta idx]
    (vec (.getArray obj)))

  java.sql.Date
  (result-set-read-column [obj result-meta idx]
    (tc/to-date obj))

  java.sql.Timestamp
  (result-set-read-column [obj result-meta idx]
    (tc/to-date-time obj)))