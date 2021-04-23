Feature: This feature is tests for errors/warnings related to implicit symbols

  Scenario Outline: Do not show arg warning for 'shell'
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    def test_fun(arg):
        pass

    test_fun() #here
    <rulelike> foo:
        run:
            shell("echo {input[1]}")
    """
    And PyArgumentListInspection inspection is enabled
    # Ensure Inspection works
    Then I expect inspection warning on <)> in <) #here> with message
    """
    Parameter 'arg' unfilled
    """
    # And no inspection warnings for shell!
    When I check highlighting warnings
    Examples:
      | rulelike   |
      | rule       |
      | checkpoint |

  Scenario Outline: Cannot find __getitem__ in section types
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    o = object()
    <rulelike> foo:
        input: "in.txt"
        output: "{sample}"
        run:
            arg_test = o[0] # here1
            arg0 = wildcards['sample'] # here1
            arg1 = <section>[0]
    """
    And PyUnresolvedReferencesInspection inspection is enabled
      # Ensure Inspection works
    Then I expect inspection warning on <[> in <[0] # here1> with message
      """
      Class 'object' does not define '__getitem__', so the '[]' operator cannot be used on its instances
      """
      # And no inspection warnings for input!
    When I check highlighting warnings
    Examples:
      | rulelike   | section |
      | rule       | input   |
      | rule       | log     |
      | rule       | output  |
      | rule       | params  |
      | checkpoint | input   |

  Scenario Outline: Cannot find get in section types
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    o = object()
    <rulelike> foo:
        input: "in.txt"
        output: "{sample}"
        run:
            arg_test = o.get(0) # here1
            arg_test = o.get(0, "1") # here2
            arg0 = wildcards.get('sample') # here3
            arg0 = wildcards.get('sample', None) # here4
            arg1 = <section>.get(0)
            arg1 = <section>.get(0, None)
    """
    And PyUnresolvedReferencesInspection inspection is enabled
      # Ensure Inspection works
    Then I expect inspection warning on <get> in <get(0) # here1> with message
      """
      Unresolved attribute reference 'get' for class 'object'
      """
    Then I expect inspection warning on <get> in <get(0, "1") # here2> with message
      """
      Unresolved attribute reference 'get' for class 'object'
      """
      # And no inspection warnings for input!
    When I check highlighting warnings
    Examples:
      | rulelike   | section |
      | rule       | input   |
      | rule       | log     |
      | rule       | output  |
      | rule       | params  |
      | checkpoint | input   |


  Scenario Outline: Cannot find config in section types
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    config
    config.items()
    config.get("d")
    config["ff"]
    config_config_config_config_config
    o = object()
    <rulelike> foo1:
        shell: "{config[d]}"
    <rulelike> foo2:
        run:
            config['ff']
            config.get("d")
            config.items()
    """
    And PyUnresolvedReferencesInspection inspection is enabled
      # Ensure Inspection works
    Then I expect inspection error on <config_config_config_config_config> with message
      """
      Unresolved reference 'config_config_config_config_config'
      """
      # And no inspection warnings for input!
    When I check highlighting warnings
    Examples:
      | rulelike   |
      | rule       |
      | checkpoint |
