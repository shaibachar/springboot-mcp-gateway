package com.shaibachar.springbootmcplib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Response model for MCP tool execution.
 * Contains the result of executing a tool, including any content and error information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolExecutionResponse {

    /**
     * List of content items returned by the tool.
     */
    private List<ContentItem> content;

    /**
     * Indicates if the execution was an error.
     */
    private Boolean isError;

    /**
     * Default constructor for JSON deserialization.
     */
    public McpToolExecutionResponse() {
    }

    /**
     * Constructor for successful execution.
     *
     * @param content the content items
     */
    public McpToolExecutionResponse(List<ContentItem> content) {
        this.content = content;
        this.isError = false;
    }

    /**
     * Constructor for error response.
     *
     * @param content the error content
     * @param isError true if this is an error response
     */
    public McpToolExecutionResponse(List<ContentItem> content, Boolean isError) {
        this.content = content;
        this.isError = isError;
    }

    /**
     * Gets the content items.
     *
     * @return the list of content items
     */
    public List<ContentItem> getContent() {
        return content;
    }

    /**
     * Sets the content items.
     *
     * @param content the content items
     */
    public void setContent(List<ContentItem> content) {
        this.content = content;
    }

    /**
     * Checks if this is an error response.
     *
     * @return true if error, false otherwise
     */
    public Boolean getIsError() {
        return isError;
    }

    /**
     * Sets the error flag.
     *
     * @param isError the error flag
     */
    public void setIsError(Boolean isError) {
        this.isError = isError;
    }

    @Override
    public String toString() {
        return "McpToolExecutionResponse{" +
                "content=" + content +
                ", isError=" + isError +
                '}';
    }

    /**
     * Represents a content item in the response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentItem {

        /**
         * The type of content (e.g., "text").
         */
        private String type;

        /**
         * The text content.
         */
        private String text;

        /**
         * Default constructor for JSON deserialization.
         */
        public ContentItem() {
        }

        /**
         * Constructor with all fields.
         *
         * @param type the content type
         * @param text the text content
         */
        public ContentItem(String type, String text) {
            this.type = type;
            this.text = text;
        }

        /**
         * Gets the content type.
         *
         * @return the content type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the content type.
         *
         * @param type the content type
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets the text content.
         *
         * @return the text content
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the text content.
         *
         * @param text the text content
         */
        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "ContentItem{" +
                    "type='" + type + '\'' +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
