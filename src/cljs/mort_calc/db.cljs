(ns mort-calc.db)

(def default-db
  {
    :loan-terms [["30" "30 Years"] ["20" "20 Years"] ["15" "15 Years"] ["10" "10 Years"]]
    :borrow-data
      {
        :amount "400000"
        :down-payment-pct "20"
        :rate "3.625"
        :term "30"
        :additional-payment "0"
        :current-month "1"
        :pmi "0"}
    :home
      {
        :value "500000"
        :property-tax "1.25"
        :start-date "2016-11-01"
        :hoa "0"}})
