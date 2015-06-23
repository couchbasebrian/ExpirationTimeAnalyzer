# ExpirationTimeAnalyzer
Utility which can analyze expiration time of documents in a bucket

When this program is run against a specific bucket, it looks at the expiration time on all documents and compares to the current time.  The difference is then charted as a histogram.

Example of use

    ===================== Welcome to Expiration Time Analyzer =====================
    === The design doc and view have been created.  Sleeping 10 seconds. ===
    === About to issue the query on the view. ===
    === totalRows is 921 ===
    === timeNow:1435095866 documentExpiry: 1436020134 timeDelta: 924268 ===
    === timeNow:1435095866 documentExpiry: 1435582657 timeDelta: 486791 ===
    === timeNow:1435095866 documentExpiry: 1435473793 timeDelta: 377927 ===
    === timeNow:1435095866 documentExpiry: 1435446133 timeDelta: 350267 ===
    === timeNow:1435095866 documentExpiry: 1435536207 timeDelta: 440341 ===
    …
    === timeNow:1435095866 documentExpiry: 1435322196 timeDelta: 226330 ===
    === timeNow:1435095866 documentExpiry: 1435774967 timeDelta: 679101 ===
    === timeNow:1435095866 documentExpiry: 1435139396 timeDelta: 43530 ===
    === timeNow:1435095866 documentExpiry: 1435999325 timeDelta: 903459 ===
    === timeNow:1435095866 documentExpiry: 1435987356 timeDelta: 891490 ===
    === numWithNullDocument:    1 ===
    === numWithExpiryException: 0 ===
    === totalResults:           921 ===

    largestVal is 107.0 spaceToUse is 62.0 dotsPerVal is 0.5794392
         0 -  99999 :     99 : ..........................................................
    100000 - 199999 :    100 : ..........................................................
    200000 - 299999 :     91 : .....................................................
    300000 - 399999 :    107 : ..............................................................
    400000 - 499999 :    101 : ...........................................................
    500000 - 599999 :     93 : ......................................................
    600000 - 699999 :    104 : .............................................................
    700000 - 799999 :     98 : .........................................................
    800000 - 899999 :    100 : ..........................................................
    900000 - 999999 :     27 : ................
           Total    :    920 : 
    Items not counted:0
    ===================== Now leaving Expiration Time Analyzer =====================
