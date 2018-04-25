function start() {
	if (args.length != 2) {
		c.getPlayer().showMessage("@str <amount>");
		return;
	}
	var amount = parseInt(args[1]);
	if (amount > 0) {
		if (amount > c.getPlayer().getRemainingAp()) {
			c.getPlayer().showMessage("You do not have enough AP.");
		} else if (amount + c.getPlayer().getStat().getStr() > 32767) {
			c.getPlayer().showMessage("You can increase your STR to a maximum of 32767.");
		} else {
			c.getPlayer().getStat().setStr(c.getPlayer().getStat().getStr() + amount, c.getPlayer());
			c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - amount);
		}
	} else if (amount < 0) {
		if (-amount > c.getPlayer().getStat().getStr() - 4) {
			c.getPlayer().showMessage("You can decrease your STR to a minimum of 4.");
		} else {
			c.getPlayer().getStat().setStr(c.getPlayer().getStat().getStr() + amount, c.getPlayer());
			c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - amount);
		}
	}
}