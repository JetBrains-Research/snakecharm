Feature: Ensures that fqn used in snakemake_api.yaml corresponds to actual resolved reference fqn
  Due to changes in snakemake module API on in PyCharm resolve impl, registered FQNs could stop working in snakemake_api.yaml.
  This test ensures, that resolve matches to what we expect.

Scenario: Function - subpath
  Given a snakemake project
  Given I open a file "foo.smk" with text
    """
    rule a:
        input:
            foo="results/something/foo.txt"
        output:
            "results/something-else/out.txt"
        params:
            directory=subpath(input.foo, parent=True)
        shell:
            "somecommand {params.directory} {output}"
    """
  When I put the caret at subpath
  Then reference should resolve to FQN "snakemake.ioutils.subpath.subpath"

  Scenario: Function - update
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
        input:
            "in.txt"
        output:
            update("test.txt")
        shell:
            "echo test >> {output}"
    """
    When I put the caret at update
    Then reference should resolve to FQN "snakemake.ioflags.update"

  Scenario: Function - before_update
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule do_something:
        input:
            before_update("test.txt")
        output:
            "in.txt"
        shell:
            "cp {input} {output}"

    rule update:
        input:
            "in.txt"
        output:
            update("test.txt")
        shell:
            "echo test >> {output}"

    """
    When I put the caret at before_update
    Then reference should resolve to FQN "snakemake.ioflags.before_update"

  Scenario: Function - exists
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
        input:
            # only expect the output if test.txt is present before workflow execution
            "out.txt" if exists("test.txt") else [],

    rule b:
        input:
            "test.txt"
        output:
            "out.txt"
        shell:
            "cp {input} {output}"
    """
    When I put the caret at exists
    Then reference should resolve to FQN "snakemake.ioutils.exists.exists"

  Scenario: Function - evaluate
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
    input:
        branch(evaluate("{sample} == '100'"), then="a/{sample}.txt", otherwise="b/{sample}.txt"),
    output:
        "c/{sample}.txt"
    """
    When I put the caret at evaluate
    Then reference should resolve to FQN "snakemake.ioutils.evaluate.evaluate"

  Scenario: Function - branch
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def use_sometool(wildcards):
        # determine whether the tool shall be used based on the wildcard values.
        pass

    rule a:
        input:
            branch(
                use_sometool,
                then="results/sometool/{dataset}.txt",
                otherwise="results/someresult/{dataset}.txt"
            )
    """
    When I put the caret at branch
    Then reference should resolve to FQN "snakemake.ioutils.branch.branch"


  Scenario: Function - lookup
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def use_sometool(wildcards):
        # determine whether the tool shall be used based on the wildcard values.
        pass

    rule a:
        input:
            branch(
                lookup(dpath="tools/sometool", within=config),,
                then="results/sometool/{dataset}.txt",
                otherwise="results/someresult/{dataset}.txt"
            )
    """
    When I put the caret at lookup
    Then reference should resolve to FQN "snakemake.ioutils.lookup.lookup"

  Scenario: Function - collect
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aggregate:
        input:
            collect("{dataset}/a.txt", dataset=DATASETS)
        output:
            "aggregated.txt"
        shell:
            ""
    """
    When I put the caret at collect
    Then reference should resolve to FQN "snakemake.io.expand"

  Scenario: Function - from_queue
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    import threading, queue, time

    # the finish sentinel
    finish_sentinel = object()
    # a synchronized queue for the input files
    all_results = queue.Queue()

    # a thread that fills the queue with input files to be considered
    def update_results():
        try:
            for i in range(10):
                all_results.put(f"test{i}.txt")
                time.sleep(1)
            all_results.put(finish_sentinel)
            all_results.join()
        except (KeyboardInterrupt, SystemExit):
            return

    update_thread = threading.Thread(target=update_results)
    update_thread.start()


    # target rule which will be continuously updated until the queue is finished
    rule all:
        input:
            from_queue(all_results, finish_sentinel=finish_sentinel)


    # job that generates the requested input files
    rule generate:
        output:
            "test{i}.txt"
        shell:
            "echo {wildcards.i} > {output}"

    """
    When I put the caret at from_queue
    Then reference should resolve to FQN "snakemake.io.from_queue"

  Scenario: Function - ensure
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output:
            ensure("test.txt", non_empty=True)
        shell:
            "somecommand {output}"

    """
    When I put the caret at ensure
    Then reference should resolve to FQN "snakemake.io.ensure"

  Scenario: Function - service
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule processor:
        output:
            service("foo.socket")
        shell:
            # here we simulate some kind of server process that provides data via a socket
            "ln -s /dev/random {output}; sleep 10000"


    rule consumer1:
        input:
            "foo.socket"
        output:
            "test.txt"
        shell:
            "head -n1 {input} > {output}"


    rule consumer2:
        input:
            "foo.socket"
        output:
            "test2.txt"
        shell:
            "head -n1 {input} > {output}"
    """
    When I put the caret at service
    Then reference should resolve to FQN "snakemake.io.service"

  Scenario: Function - report
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule d:
        output:
            report(
                directory("testdir"),
                patterns=["{name}.txt"],
                caption="results/somedata.rst",
                category="Step 3")
        shell:
            "mkdir {output}; for i in 1 2 3; do echo $i > {output}/$i.txt; done"
    """
    When I put the caret at report
    Then reference should resolve to FQN "snakemake.io.report"

  Scenario: Function - pipe
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
        input:
            expand("test.{i}.out", i=range(2))


    rule a:
        output:
            pipe("test.{i}.txt")
        shell:
            "for i in {{0..2}}; do echo {wildcards.i} >> {output}; done"


    rule b:
        input:
            "test.{i}.txt"
        output:
            "test.{i}.out"
        shell:
            "grep {wildcards.i} < {input} > {output}"

    """
    When I put the caret at pipe
    Then reference should resolve to FQN "snakemake.io.pipe"

  Scenario: Function - ancient
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input:
            ancient("path/to/inputfile")
        output:
            "path/to/outputfile"
        shell:
            "somecommand {input} {output}"
    """
    When I put the caret at ancient
    Then reference should resolve to FQN "snakemake.io.ancient"

  Scenario: Function - protected
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input:
            "path/to/inputfile"
        output:
            protected("path/to/outputfile")
        shell:
            "somecommand {input} {output}"
    """
    When I put the caret at protected
    Then reference should resolve to FQN "snakemake.io.protected"

  Scenario: Function - directory
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input:
            "path/to/inputfile"
        output:
            directory("path/to/outputdir")
        shell:
            "somecommand {input} {output}"
    """
    When I put the caret at directory
    Then reference should resolve to FQN "snakemake.io.directory"

  Scenario: Function - temp
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input:
            "path/to/inputfile"
        output:
            temp("path/to/outputfile")
        shell:
            "somecommand {input} {output}"

    """
    When I put the caret at temp
    Then reference should resolve to FQN "snakemake.io.temp"

  Scenario: Function - temporary
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        input:
            "path/to/inputfile"
        output:
            temporary("path/to/outputfile")
        shell:
            "somecommand {input} {output}"

    """
    When I put the caret at temporary
    Then reference should resolve to FQN "snakemake.io.temporary"

  Scenario: Function - touch
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
        input: "mytask.done"

    rule mytask:
        output: touch("mytask.done")
        shell: "mycommand ..."
    """
    When I put the caret at touch
    Then reference should resolve to FQN "snakemake.io.touch"

  Scenario: Function - unpack
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def myfunc(wildcards):
        return {'foo': '{wildcards.token}.txt'.format(wildcards=wildcards)}

    rule:
        input:
            unpack(myfunc)
        output:
            "someoutput.{token}.txt"
        shell:
            "..."

    """
    When I put the caret at unpack
    Then reference should resolve to FQN "snakemake.io.unpack"

  Scenario: Function - repeat
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule bwa_map:
        benchmark:
           repeat("benchmarks/{sample}.bwa.benchmark.txt", 3)
    """
    When I put the caret at repeat
    Then reference should resolve to FQN "snakemake.io.repeat"

  Scenario: Function - local
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule example:
        input:
            local("resources/example-input.txt")
    """
    When I put the caret at local
    Then reference should resolve to FQN "snakemake.io.local"

  Scenario: Function - expand
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aggregate:
        input:
            expand("{dataset}/a.{ext}", dataset=DATASETS, ext=FORMATS)
        output:
            "aggregated.txt"
        shell:
            "..."
    """
    When I put the caret at expand
    Then reference should resolve to FQN "snakemake.io.expand"

  Scenario: Function - multiext
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aggregate:
      output:
          multiext("some/plot", ".pdf", ".svg", ".png")
    """
    When I put the caret at multiext
    Then reference should resolve to FQN "snakemake.io.multiext"
