package GameOfLife.example.controller;

import GameOfLife.example.entity.Game;
import GameOfLife.example.entity.Offer;
import GameOfLife.example.entity.Profil;
import GameOfLife.example.json.JsonOffer;
import GameOfLife.example.logik.OfferState;
import GameOfLife.example.repository.GameRepository;
import GameOfLife.example.repository.OfferRepository;
import GameOfLife.example.repository.ProfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by raedschk on 10.08.2016.
 */
@Controller
public class boerseController {
    @Autowired
    private OfferRepository oRepo;

    @Autowired
    private GameRepository gRepo;

    @Autowired
    private ProfilRepository pRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ApplicationContext ctx;

    @MessageMapping("/boerse/start")
    public void start(Principal principal) throws Exception {
        this.messagingTemplate.convertAndSendToUser(principal.getName(), "/out/boerse/list", getOfferList());
    }

    @MessageMapping("/boerse/add")
    public void add(Principal principal) throws Exception {
        Offer o = oRepo.findOne(principal.getName());
        if(o == null)
        {
            o = new Offer(principal.getName());
            oRepo.save(o);
            boerseChanged();
        }
    }

    @MessageMapping("/boerse/remove")
    public void remove(Principal principal) throws Exception {
        if(oRepo.exists(principal.getName()))
        {
            oRepo.delete(principal.getName());
            boerseChanged();
        }
    }

    @MessageMapping("/boerse/accept")
    public void accept(String username, Principal principal) throws Exception {
        Game g1 = gRepo.findOneByPlayer1OrPlayer2(principal.getName(), principal.getName());
        Game g2 = gRepo.findOneByPlayer1OrPlayer2(username, username);
        if(username != principal.getName() && oRepo.exists(username) && g1 == null && g2 == null)
        {
            Profil p = pRepo.findOne(principal.getName());
            int[][] board = new int[p.getHeight()][p.getWidth()];
            for(int y = 0; y < p.getHeight(); y++){
                for(int x = 0; x < p.getWidth(); x++){
                    board[y][x] = 0;
                }
            }
            gRepo.save(new Game(1, username, principal.getName(), board));
            oRepo.delete(username);

            this.messagingTemplate.convertAndSendToUser(username, "/out/boerse/game", null);
            this.messagingTemplate.convertAndSendToUser(principal.getName(), "/out/boerse/game", null);
        }

    }

    private void boerseChanged()
    {
        this.messagingTemplate.convertAndSend("/out/boerse/list", getOfferList());
    }

    private List<JsonOffer> getOfferList()
    {
        Iterable<Offer> list = oRepo.findAll();
        List<JsonOffer> offers = new ArrayList<JsonOffer>();

        for(Offer offer : list)
        {
            JsonOffer jsonOffer = new JsonOffer(offer.getUsernname(), "GEGNER", OfferState.Available);
            offers.add(jsonOffer);
        }

        return offers;
    }
}