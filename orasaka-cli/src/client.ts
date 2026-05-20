import { GraphQLClient, gql } from 'graphql-request';

/**
 * Orasaka CLI Client
 * Foundation for terminal-based AI orchestration.
 */
export class OrasakaCliClient {
    private client: GraphQLClient;

    constructor(private endpoint: string = 'http://localhost:8080/graphql') {
        this.client = new GraphQLClient(this.endpoint);
    }

    /**
     * Executes a chat mutation.
     * 
     * @param prompt The user input.
     * @param conversationId Optional session ID.
     */
    async chat(prompt: string, conversationId?: string) {
        const mutation = gql`
            mutation Chat($prompt: String!, $conversationId: String) {
                chat(prompt: $prompt, conversationId: $conversationId) {
                    content
                    conversationId
                }
            }
        `;

        try {
            const data: any = await this.client.request(mutation, { prompt, conversationId });
            return data.chat;
        } catch (error) {
            console.error('Orasaka CLI Error:', error);
            throw error;
        }
    }

    /**
     * Executes a chat stream subscription.
     * 
     * @param prompt The user input.
     * @param conversationId Optional session ID.
     * @param onNext Callback for each streamed token.
     * @param onError Callback for errors.
     * @param onComplete Callback when the stream completes.
     */
    async chatStream(prompt: string, conversationId?: string, onNext?: (data: any) => void, onError?: (error: any) => void, onComplete?: () => void) {
        const { createClient } = await import('graphql-ws');
        const WebSocket = await import('ws');

        const wsEndpoint = this.endpoint.replace(/^http/, 'ws');
        
        const wsClient = createClient({
            url: wsEndpoint,
            webSocketImpl: WebSocket.default || WebSocket
        });

        const query = `
            subscription ChatStream($prompt: String!, $conversationId: String) {
                chatStream(prompt: $prompt, conversationId: $conversationId) {
                    content
                    conversationId
                }
            }
        `;

        wsClient.subscribe(
            {
                query,
                variables: { prompt, conversationId }
            },
            {
                next: (data) => {
                    if (onNext) onNext(data.data?.chatStream);
                },
                error: (err) => {
                    if (onError) onError(err);
                },
                complete: () => {
                    if (onComplete) onComplete();
                }
            }
        );
    }
}
