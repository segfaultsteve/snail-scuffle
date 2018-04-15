package com.snailscuffle.game.battle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MeteredStatTest {
	
	private static final int INITIAL_VALUE = 100;
	
	@Mock private Combatant player;
	@Mock private Combatant opponent;
	private MeteredStat stat;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		stat = new MeteredStat(player, INITIAL_VALUE);
		stat.registerOpponentForCallbacks(opponent);
	}

	@Test
	public void testSet() {
		stat.set(50);
		assertEquals(50, stat.get());
		verify(player).onStatChanged();
		verify(opponent).onEnemyStatChanged();
	}
	
	@Test
	public void testAdd() {
		stat.add(10);
		assertEquals(INITIAL_VALUE + 10, stat.get());
		verify(player).onStatChanged();
		verify(opponent).onEnemyStatChanged();
	}
	
	@Test
	public void testSubtract() {
		stat.subtract(10);
		assertEquals(INITIAL_VALUE - 10, stat.get());
		verify(player).onStatChanged();
		verify(opponent).onEnemyStatChanged();
	}

}
