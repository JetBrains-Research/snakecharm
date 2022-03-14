Feature: Resolve after config keyword

  Scenario Outline: Resolve for top-level keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['<text>'])
    """
    When I put the caret after ['
    Then reference should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key1 |
      | custom_key2 |

  Scenario Outline: Resolve for top-level keys, SL case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      shell: "{config[<text>]}"
    """
    When I put the caret after [
    Then reference in injection should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key1 |
      | custom_key2 |

  Scenario Outline: Resolve for non top-level keys mapping
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
      custom_key4:
        value2
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['<text>'])
    """
    When I put the caret after ]['
    Then reference should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key3 |
      | custom_key4 |

  Scenario Outline: Resolve for non top-level keys mapping, SL case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
      custom_key4:
        value2
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      shell: "{config[custom_key2][<text>]}"
    """
    When I put the caret after ][
    Then reference in injection should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key3 |
      | custom_key4 |

  Scenario Outline: Resolve for non top-level keys mapping after sequence
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5: value4
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][<index>]['<text>'])
    """
    When I put the caret after [<index>]['
    Then reference should resolve to "<text>" in "config.yaml"
    Examples:
      | text        | index |
      | custom_key4 | 0     |
      | custom_key5 | 1     |

  Scenario Outline: Resolve for non top-level keys mapping after sequence, SL case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5: value4
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      shell: "{config[custom_key2][custom_key3][<index>][<text>]}"
    """
    When I put the caret after [<index>][
    Then reference in injection should resolve to "<text>" in "config.yaml"
    Examples:
      | text        | index |
      | custom_key4 | 0     |
      | custom_key5 | 1     |

  Scenario Outline: Resolve for non top-level keys, mapping after sequence
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5:
            custom_key6: value4
            custom_key7: value4
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][1]['custom_key5']['<text>'])
    """
    When I put the caret after ['custom_key5']['
    Then reference should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key6 |
      | custom_key7 |

  Scenario Outline: Resolve for non top-level keys, mapping after sequence, SL case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5:
            custom_key6: value4
            custom_key7: value4
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      shell: "{print(config[custom_key2][custom_key3][1][custom_key5][<text>]}"
    """
    When I put the caret after [custom_key5][
    Then reference in injection should resolve to "<text>" in "config.yaml"
    Examples:
      | text        |
      | custom_key6 |
      | custom_key7 |