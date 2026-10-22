package co.edu.kh.spring_authorization_server.repository;

import co.edu.kh.spring_authorization_server.mapper.RegisteredClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Repository
@Transactional
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {


    private final ClientRepository clientRepository;
    private final RegisteredClientMapper mapper;

    @Override
    public void save(RegisteredClient client) {
        Assert.notNull(client, "client cannot be null");
        clientRepository.save(mapper.toEntity(client));
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return clientRepository.findById(id)
                .map(mapper::toRegisteredClient)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return clientRepository.findByClientId(clientId)
                .map(mapper::toRegisteredClient)
                .orElse(null);
    }


}
