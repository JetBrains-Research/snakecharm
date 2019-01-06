onstart:
    foo("Workflow started, no error")

onsuccess:
    foo("Workflow finished, no error")

onerror:
    foo("An error occurred")
    shell("mail -s \"an error occurred\" youremail@provider.com < {log}")

onstart:  foo("Workflow started, no error"); boo()


def boo():
  onsuccess:
      foo("AAAA")
