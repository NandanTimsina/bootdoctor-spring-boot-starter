package com.nandantimsina.bootdoctor;

public interface LlmClient {

    /** Sends the prompt and returns the model's text answer. Throws LlmException on any failure. */
    String complete(PromptBuilder.Prompt prompt);
}