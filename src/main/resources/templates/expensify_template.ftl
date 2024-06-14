[
<#lt>
<#list reports as report>
  {
    "reportID": "${report.reportID}",
    "reportName": "${report.reportName}",
    "accountEmail": "${report.accountEmail}",
    "reportCreated": "${report.created}",
    "transactionList": [
      <#list report.transactionList as expense>
        {
          "merchant": "${expense.merchant}",
          "amount": ${expense.amount/100},
          "category": "${expense.category}",
          "Reciept": "${expense.receiptObject.url}"
        }<#if expense?has_next>,</#if>
      </#list>
    ]
  }<#if report?has_next>,</#if>
</#list>
]