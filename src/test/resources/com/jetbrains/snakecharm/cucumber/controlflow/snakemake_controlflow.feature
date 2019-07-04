Feature: Documentation for 'shadow' settings

  # TODO: Required for https://github.com/JetBrains-Research/snakecharm/issues/14
#  Scenario: Control flow for onerror
#    Given a snakemake project
#    Given I open a file "foo.smk" with text
#    """
#    onerror:
#        return 1
#    print()
#    """
#    Then I expect controlflow
#    """
#    0(1) element: null
#    1(2) element: SMKWorkflowPythonBlockParameter
#    2(3) WRITE ACCESS: null
#    3(4) element: PyExpressionStatement
#    4(5) READ ACCESS: print
#    5() element: null
#    """