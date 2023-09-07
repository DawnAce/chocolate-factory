package cc.unitmesh.cf.domains.testcase.flow

import cc.unitmesh.cf.core.dsl.Dsl
import cc.unitmesh.cf.core.dsl.DslInterpreter
import cc.unitmesh.cf.core.process.SolutionDesigner
import cc.unitmesh.cf.domains.testcase.TestcaseWorkflow
import cc.unitmesh.cf.domains.testcase.context.TestcaseVariableResolver
import cc.unitmesh.cf.infrastructure.llms.completion.LlmProvider
import cc.unitmesh.cf.infrastructure.llms.model.LlmMsg
import cc.unitmesh.cf.infrastructure.parser.MarkdownCode

class TestcaseSolutionDesigner(
    private val completion: LlmProvider,
    private val variable: TestcaseVariableResolver,
) : SolutionDesigner {
    override fun design(domain: String, question: String, histories: List<String>): Dsl {
        variable.updateQuestion(question)
        variable.updateHistories(histories)

        // parse code from histories
        var testcases = ""
        histories.filter { it.isNotBlank() }.forEach {
            val parse = MarkdownCode.parse(it)
            if (parse.language == "testcases") {
                testcases = parse.text
            }
        }
        variable.put("testcases", testcases)
        val prompt = TestcaseWorkflow.DESIGN.format()

        val messages = listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.System, variable.compile(prompt)),
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, question),
        ).filter { it.content.isNotBlank() }

        log.info("messages: {}", messages)
        val completion = completion.simpleCompletion(messages)
        log.info("completion: {}", completion)

        return object : Dsl {
            override var domain: String = domain
            override val content: String = completion
            override var interpreters: List<DslInterpreter> = listOf()
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(TestcaseSolutionDesigner::class.java)
    }
}