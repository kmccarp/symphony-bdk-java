package com.symphony.bdk.core.service.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.retry.RetryWithRecoveryBuilder;
import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.pagination.model.StreamPaginationAttribute;
import com.symphony.bdk.core.test.MockApiClient;
import com.symphony.bdk.gen.api.SignalsApi;
import com.symphony.bdk.gen.api.model.BaseSignal;
import com.symphony.bdk.gen.api.model.ChannelSubscriber;
import com.symphony.bdk.gen.api.model.ChannelSubscriptionResponse;
import com.symphony.bdk.gen.api.model.Signal;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiRuntimeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignalServiceTest {

  private static final String V1_LIST_SIGNAL = "/agent/v1/signals/list";
  private static final String V1_GET_SIGNAL = "/agent/v1/signals/{id}/get";
  private static final String V1_CREATE_SIGNAL = "/agent/v1/signals/create";
  private static final String V1_UPDATE_SIGNAL = "/agent/v1/signals/{id}/update";
  private static final String V1_DELETE_SIGNAL = "/agent/v1/signals/{id}/delete";
  private static final String V1_SUBSCRIBE_SIGNAL = "/agent/v1/signals/{id}/subscribe";
  private static final String V1_UNSUBSCRIBE_SIGNAL = "/agent/v1/signals/{id}/unsubscribe";
  private static final String V1_SUBSCRIBERS = "/agent/v1/signals/{id}/subscribers";

  private SignalService service;
  private SignalsApi spiedSignalApi;
  private MockApiClient mockApiClient;
  private AuthSession authSession;

  @BeforeEach
  void init() {
    this.mockApiClient = new MockApiClient();
    this.authSession = mock(AuthSession.class);
    this.spiedSignalApi = spy(new SignalsApi(mockApiClient.getApiClient("/agent")));
    this.service = new SignalService(spiedSignalApi, authSession, new RetryWithRecoveryBuilder<>());

    when(authSession.getSessionToken()).thenReturn("1234");
    when(authSession.getKeyManagerToken()).thenReturn("1234");
  }

  @Test
  void nonOboEndpointShouldThrowExceptionInOboMode() {
    service = new SignalService(spiedSignalApi, new RetryWithRecoveryBuilder<>());

    assertThrows(IllegalStateException.class, () -> service.getSignal(""));
  }

  @Test
  void testGetSignalOboMode() {
    this.mockApiClient.onGet(V1_GET_SIGNAL.replace("{id}", "1234"),
        """
        {
            "name": "my signal",
            "query": "HASHTAG:hashtag AND CASHTAG:cash",
            "visibleOnProfile": true,
            "companyWide": false,
            "id": "5a8daa0bb9d82100011d5095",
            "timestamp": 1519233547982
        }\
        """);

    this.service = new SignalService(this.spiedSignalApi, new RetryWithRecoveryBuilder<>());
    final Signal signal = this.service.obo(this.authSession).getSignal("1234");

    assertEquals(signal.getId(), "5a8daa0bb9d82100011d5095");
  }

  @Test
  void listSignalTest() {
    this.mockApiClient.onGet(V1_LIST_SIGNAL,
        """
        [
          {
            "name": "Mention and keyword",
            "query": "HASHTAG:Hello OR POSTEDBY:10854618893681",
            "visibleOnProfile": false,
            "companyWide": false,
            "id": "5a0068344b570777718322a3",
            "timestamp": 1509976116525
          }
        ]\
        """);

    List<Signal> signals = this.service.listSignals(new PaginationAttribute(0, 100));

    assertEquals(signals.size(), 1);
    assertEquals(signals.get(0).getId(), "5a0068344b570777718322a3");
    assertEquals(signals.get(0).getName(), "Mention and keyword");
  }

  @Test
  void listSignalsDefaultLimit() {
    this.mockApiClient.onGet(V1_LIST_SIGNAL,
        """
        [
          {
            "name": "Mention and keyword",
            "query": "HASHTAG:Hello OR POSTEDBY:10854618893681",
            "visibleOnProfile": false,
            "companyWide": false,
            "id": "5a0068344b570777718322a3",
            "timestamp": 1509976116525
          }
        ]\
        """);

    List<Signal> signals = this.service.listSignals();

    assertEquals(signals.size(), 1);
    assertEquals(signals.get(0).getId(), "5a0068344b570777718322a3");
    assertEquals(signals.get(0).getName(), "Mention and keyword");
  }

  @Test
  void listSignalFailed() {
    this.mockApiClient.onGet(400, V1_LIST_SIGNAL, "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.listSignals(new PaginationAttribute(0, 100)));
  }

  @Test
  void listSignalStreamTest() {
    this.mockApiClient.onGet(V1_LIST_SIGNAL,
        """
        [
          {
            "name": "Mention and keyword",
            "query": "HASHTAG:Hello OR POSTEDBY:10854618893681",
            "visibleOnProfile": false,
            "companyWide": false,
            "id": "5a0068344b570777718322a3",
            "timestamp": 1509976116525
          }
        ]\
        """);

    Stream<Signal> signals = this.service.listAllSignals(new StreamPaginationAttribute(2, 2));
    List<Signal> signalList = signals.collect(Collectors.toList());

    assertEquals(signalList.size(), 1);
    assertEquals(signalList.get(0).getQuery(), "HASHTAG:Hello OR POSTEDBY:10854618893681");
  }

  @Test
  void listSignalStreamDefaultPagination() {
    this.mockApiClient.onGet(V1_LIST_SIGNAL,
        """
        [
          {
            "name": "Mention and keyword",
            "query": "HASHTAG:Hello OR POSTEDBY:10854618893681",
            "visibleOnProfile": false,
            "companyWide": false,
            "id": "5a0068344b570777718322a3",
            "timestamp": 1509976116525
          }
        ]\
        """);

    Stream<Signal> signals = this.service.listAllSignals();
    List<Signal> signalList = signals.collect(Collectors.toList());

    assertEquals(signalList.size(), 1);
    assertEquals(signalList.get(0).getQuery(), "HASHTAG:Hello OR POSTEDBY:10854618893681");
  }

  @Test
  void getSignalTest() {
    this.mockApiClient.onGet(V1_GET_SIGNAL.replace("{id}", "1234"),
        """
        {
            "name": "my signal",
            "query": "HASHTAG:hashtag AND CASHTAG:cash",
            "visibleOnProfile": true,
            "companyWide": false,
            "id": "5a8daa0bb9d82100011d5095",
            "timestamp": 1519233547982
        }\
        """);

    Signal signal = this.service.getSignal("1234");

    assertEquals(signal.getId(), "5a8daa0bb9d82100011d5095");
    assertEquals(signal.getQuery(), "HASHTAG:hashtag AND CASHTAG:cash");
    assertFalse(signal.getCompanyWide());
  }

  @Test
  void getSignalFailed() {
    this.mockApiClient.onGet(400, V1_GET_SIGNAL.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.getSignal("1234"));
  }

  @Test
  void createSignalTest() {
    this.mockApiClient.onPost(V1_CREATE_SIGNAL,
        """
        {
            "name": "hash and cash",
            "query": "HASHTAG:hash AND CASHTAG:cash",
            "visibleOnProfile": true,
            "companyWide": false,
            "id": "5a8da7edb9d82100011d508f",
            "timestamp": 1519233005107
        }\
        """);

    Signal signal = this.service.createSignal(new BaseSignal());

    assertEquals(signal.getId(), "5a8da7edb9d82100011d508f");
    assertEquals(signal.getQuery(), "HASHTAG:hash AND CASHTAG:cash");
    assertFalse(signal.getCompanyWide());
  }

  @Test
  void createSignalFailed() {
    this.mockApiClient.onPost(400, V1_CREATE_SIGNAL, "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.createSignal(new BaseSignal()));
  }

  @Test
  void updateSignalTest() {
    this.mockApiClient.onPost(V1_UPDATE_SIGNAL.replace("{id}", "1234"),
        """
        {
            "name": "hashtag only",
            "query": "HASHTAG:hash",
            "visibleOnProfile": false,
            "companyWide": false,
            "id": "1234",
            "timestamp": 1519233005107
        }\
        """);

    Signal signal = this.service.updateSignal("1234", new BaseSignal());

    assertEquals(signal.getQuery(), "HASHTAG:hash");
    assertFalse(signal.getVisibleOnProfile());
    assertFalse(signal.getCompanyWide());
  }

  @Test
  void updateSignalFailed() {
    this.mockApiClient.onPost(400, V1_UPDATE_SIGNAL.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.updateSignal("1234", new BaseSignal()));
  }

  @Test
  void deleteSignalTest() throws ApiException {
    this.mockApiClient.onPost(V1_DELETE_SIGNAL.replace("{id}", "signal-id"), "{}");

    this.service.deleteSignal("signal-id");

    verify(this.spiedSignalApi).v1SignalsIdDeletePost("1234", "signal-id", "1234");
  }

  @Test
  void deleteSignalFailed() {
    this.mockApiClient.onPost(400, V1_DELETE_SIGNAL.replace("{id}", "signal-id"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.deleteSignal("signal-id"));
  }

  @Test
  void subscribeSignalTest() throws ApiException {
    this.mockApiClient.onPost(V1_SUBSCRIBE_SIGNAL.replace("{id}", "1234"),
        """
        {
            "requestedSubscription": 3,
            "successfulSubscription": 3,
            "failedSubscription": 0,
            "subscriptionErrors": []
        }\
        """);

    ChannelSubscriptionResponse response =
        this.service.subscribeUsersToSignal("1234", true, Arrays.asList(1234L, 1235L, 1236L));

    verify(spiedSignalApi).v1SignalsIdSubscribePost("1234", "1234", "1234", true, Arrays.asList(1234L, 1235L, 1236L));
    assertEquals(response.getRequestedSubscription(), 3);
    assertEquals(response.getSuccessfulSubscription(), 3);
  }

  @Test
  void subscribeSignalFailed() {
    this.mockApiClient.onPost(400, V1_SUBSCRIBE_SIGNAL.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.subscribeUsersToSignal("1234", true, Collections.singletonList(1234L)));
  }

  @Test
  void unsubscribeSignalTest() throws ApiException {
    this.mockApiClient.onPost(V1_UNSUBSCRIBE_SIGNAL.replace("{id}", "1234"),
        """
        {
            "requestedSubscription": 3,
            "successfulSubscription": 3,
            "failedSubscription": 0,
            "subscriptionErrors": []
        }\
        """);

    ChannelSubscriptionResponse response =
        this.service.unsubscribeUsersFromSignal("1234", Arrays.asList(1234L, 1235L, 1236L));

    verify(spiedSignalApi).v1SignalsIdUnsubscribePost("1234", "1234", "1234", Arrays.asList(1234L, 1235L, 1236L));
    assertEquals(response.getSuccessfulSubscription(), 3);
    assertEquals(response.getRequestedSubscription(), 3);
  }

  @Test
  void unsubscribeSignalFailed() {
    this.mockApiClient.onPost(400, V1_UNSUBSCRIBE_SIGNAL.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.unsubscribeUsersFromSignal("1234", Collections.singletonList(1234L)));
  }

  @Test
  void subscribersTest() {
    this.mockApiClient.onGet(V1_SUBSCRIBERS.replace("{id}", "1234"),
        """
        {
            "offset": 0,
            "hasMore": true,
            "total": 150,
            "data": [
                {
                    "pushed": false,
                    "owner": true,
                    "subscriberName": "John Doe 01",
                    "userId": 68719476742,
                    "timestamp": 1519231972000
                }
            ]
        }\
        """);

    List<ChannelSubscriber> subscribers = this.service.listSubscribers("1234", new PaginationAttribute(0, 100));

    assertEquals(subscribers.size(), 1);
    assertEquals(subscribers.get(0).getUserId(), 68719476742L);
    assertEquals(subscribers.get(0).getSubscriberName(), "John Doe 01");
  }

  @Test
  void subscribersDefaultLimit() {
    this.mockApiClient.onGet(V1_SUBSCRIBERS.replace("{id}", "1234"),
        """
        {
            "offset": 0,
            "hasMore": true,
            "total": 150,
            "data": [
                {
                    "pushed": false,
                    "owner": true,
                    "subscriberName": "John Doe 01",
                    "userId": 68719476742,
                    "timestamp": 1519231972000
                }
            ]
        }\
        """);

    List<ChannelSubscriber> subscribers = this.service.listSubscribers("1234");

    assertEquals(subscribers.size(), 1);
    assertEquals(subscribers.get(0).getUserId(), 68719476742L);
    assertEquals(subscribers.get(0).getSubscriberName(), "John Doe 01");
  }

  @Test
  void subscribersFailed() {
    this.mockApiClient.onGet(400, V1_SUBSCRIBERS.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.listSubscribers("1234", new PaginationAttribute(0, 100)));
  }

  @Test
  void subscribersStreamTest() {
    this.mockApiClient.onGet(V1_SUBSCRIBERS.replace("{id}", "1234"),
        """
        {
            "offset": 0,
            "hasMore": true,
            "total": 150,
            "data": [
                {
                    "pushed": false,
                    "owner": true,
                    "subscriberName": "John Doe 01",
                    "userId": 68719476742,
                    "timestamp": 1519231972000
                }
            ]
        }\
        """);

    Stream<ChannelSubscriber> subscribers =
        this.service.listAllSubscribers("1234", new StreamPaginationAttribute(2, 2));
    List<ChannelSubscriber> subscriberList = subscribers.collect(Collectors.toList());

    assertEquals(subscriberList.size(), 1);
    assertEquals(subscriberList.get(0).getUserId(), 68719476742L);
    assertEquals(subscriberList.get(0).getSubscriberName(), "John Doe 01");
  }

  @Test
  void subscribersStreamDefaultPagination() {
    this.mockApiClient.onGet(V1_SUBSCRIBERS.replace("{id}", "1234"),
        """
        {
            "offset": 0,
            "hasMore": true,
            "total": 150,
            "data": [
                {
                    "pushed": false,
                    "owner": true,
                    "subscriberName": "John Doe 01",
                    "userId": 68719476742,
                    "timestamp": 1519231972000
                }
            ]
        }\
        """);

    Stream<ChannelSubscriber> subscribers = this.service.listAllSubscribers("1234");
    List<ChannelSubscriber> subscriberList = subscribers.collect(Collectors.toList());

    assertEquals(subscriberList.size(), 1);
    assertEquals(subscriberList.get(0).getUserId(), 68719476742L);
    assertEquals(subscriberList.get(0).getSubscriberName(), "John Doe 01");
  }
}
