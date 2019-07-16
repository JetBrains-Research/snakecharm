if True:
    configfile: "ss1"
elif True:
    configfile: "ss2"
else:
    configfile: "ss3"

try:
    configfile: "ss4"
except:
    configfile: "ss5"

def foo():
    rule boo:
        input: "in"

