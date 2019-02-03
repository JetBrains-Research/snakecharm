rule foo:<fold text='...'>
    input:
       a = "aaa",
       b = "bbb"
    output: "ooo"
    params: c = "ccc"
    run:<fold text='...'>
       print()
       print()
       print()</fold></fold>
